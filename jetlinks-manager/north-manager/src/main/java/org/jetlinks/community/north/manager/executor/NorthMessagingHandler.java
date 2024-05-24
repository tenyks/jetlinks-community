package org.jetlinks.community.north.manager.executor;

import com.alibaba.fastjson.JSONObject;
import org.apache.activemq.AlreadyClosedException;
import org.apache.activemq.ConnectionClosedException;
import org.apache.commons.lang3.StringUtils;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.north.manager.message.NorthMessage;
import org.jetlinks.core.jms.ActiveMQClientImpl;
import org.jetlinks.core.jms.JMSClient;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 北向消息推送任务
 */
@Component @ConditionalOnProperty(value = "system.config.north.enabled", havingValue = "true", matchIfMissing = false)
public class NorthMessagingHandler implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(NorthMessagingHandler.class);

    private static final int    JMS_COMMIT_BATCH_SIZE = 20;
    private static final long   SLEEP_SHORT_TIME = 1000;
    private static final long   SLEEP_LONG_TIME = 30000;

    private final String    jmsBrokerUrl;

    private final String    jmsQueueUri;

    private final NorthMessagingTaskExecutor    executor;

    public NorthMessagingHandler(@Value("${system.config.north.jmsBrokerUrl:}") String jmsBrokerUrl,
                                 @Value("${system.config.north.jmsQueueUri:}") String jmsQueueUri) {
        this.jmsBrokerUrl = jmsBrokerUrl;
        this.jmsQueueUri = StringUtils.isNotBlank(jmsQueueUri) ? jmsQueueUri : "/iot/north/message";

        this.executor = new NorthMessagingTaskExecutor();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isBlank(jmsBrokerUrl)) {
            log.warn("[NorthMessaging]没有配置北向消息JMS Broker地址， 不推送北向消息");
            return ;
        }

        this.executor.doStart();
    }

    @Override
    public void destroy() throws Exception {
        if (!this.executor.isDisposed()) {
            this.executor.dispose();
        }
    }

    @Subscribe(topics = {
        "/device/*/*/register", "/device/*/*/unregister",
        "/device/*/*/online", "/device/*/*/offline",
        "/device/*/*/message/property/report",
        "/device/*/*/message/property/read,write/reply",
        "/device/*/*/message/event/*"
    })
    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> handleNorthMessage(DeviceMessage message) {
        if (this.executor.isDisposed()) {
            if (log.isWarnEnabled()) {
                log.warn("[NorthMessaging]北向推送已关闭，不接受消息：event={}", message.toJson());
            }
            return Mono.empty();
        }

        return this.executor.submitMessage(message);
    }

    class NorthMessagingTaskExecutor implements Runnable, Disposable {

        private JMSClient   jmsClient;

        /**
         * 待发往北向对接JMS队列的消息
         */
        private final BlockingQueue<DeviceMessage> waitToPushQueue;

        private final AtomicBoolean     stop = new AtomicBoolean(false);

        private Thread                  attachedThread = null;

        public NorthMessagingTaskExecutor() {
            this.waitToPushQueue = new LinkedBlockingDeque<>();
        }

        protected Disposable doStart() {
            attachedThread = new Thread(this, "north-pushing-thread");
            attachedThread.start();

            return this;
        }

        @Override
        public boolean isDisposed() {
            return stop.get();
        }

        @Override
        public void dispose() {
            this.stop.set(true);
            this.attachedThread.interrupt();
        }

        public Mono<Void> submitMessage(DeviceMessage message) {
            return Mono.defer(() -> {
                waitToPushQueue.add(message);
                return Mono.empty();
            });
        }

        @Override
        public void run() {
            log.warn("[NorthMessaging]等待初始化完成，30秒后开启推送...");
            sleepAMoment(30000);
            log.info("[NorthMessaging]开始北向消息推送...");

            int idx = 0;
            List<DeviceMessage> bufList = new ArrayList<>(JMS_COMMIT_BATCH_SIZE + 1);

            while (!stop.get()) {
                if (bufList.isEmpty()) {
                    // 使用平衡高吞吐和高响应延时的方式读取queue
                    if (waitToPushQueue.isEmpty()) {
                        try {
                            DeviceMessage msg = waitToPushQueue.poll(30, TimeUnit.SECONDS);
                            if(msg != null) {
                                bufList.add(msg);
                            } else {
                                log.info("[NorthMessaging]待发送的北向消息队列为空");
                                continue;
                            }
                        } catch (InterruptedException e) {
                            log.info("[NorthMessaging]等待消息被中断：", e);
                            continue;
                        }
                    }

                    waitToPushQueue.drainTo(bufList, Math.min(waitToPushQueue.size(), JMS_COMMIT_BATCH_SIZE));
                }

                idx = pushingBatchMessage(bufList, idx);
                if (idx >= bufList.size()) {
                    log.info("[NorthMessaging]推送了{}条消息", bufList.size());

                    idx = 0;
                    bufList.clear();
                }
            }

            log.warn("[NorthMessaging]北向消息推送已终止。");
        }

        private int pushingBatchMessage(List<DeviceMessage> msgList, int srcIdx) {
            if (msgList.isEmpty()) return srcIdx;

            if (jmsClient == null) {
                try {
                    jmsClient = new ActiveMQClientImpl("iot-north-messaging", jmsBrokerUrl);
                    log.info("[NorthMessaging]建立JMS链接成功：{}", jmsClient);
                } catch (JMSException e) {
                    log.error("[NorthMessaging]JMS建立链接失败：url={}，请检查链接配置或JMS服务器", jmsBrokerUrl, e);
                    sleepAMoment(SLEEP_LONG_TIME);
                    return srcIdx;
                }
            }

            boolean reconnectClient = false;
            int idx = srcIdx;
            for (; idx < msgList.size(); idx++) {
                DeviceMessage msg = msgList.get(idx);

                NorthMessage northMsg = NorthMessage.fromMessage(msg);
                if (northMsg == null) {
                    if (log.isWarnEnabled()) {
                        log.warn("[NorthMessaging]忽略消息：{}", msg.toJson());
                    }
                    continue;
                }

                String payloadStr = JSONObject.toJSONString(northMsg);
                try {
                    jmsClient.send(jmsQueueUri, payloadStr);
                    if (log.isInfoEnabled()) {
                        log.info("[NorthMessaging]JMS消息已发送：{}", payloadStr);
                    }
                } catch (ConnectionClosedException | AlreadyClosedException e) {
                    reconnectClient = true;
                    log.error("[NorthMessaging]JMS链接发现异常：", e);
                    break;
                } catch (JMSException e) {
                    log.error("[NorthMessaging]JMS发送异常，忽略消息：{}", payloadStr, e);
                    sleepAMoment(SLEEP_SHORT_TIME);
                    continue;
                }
            }

            if (!reconnectClient) {
                // 保证消息最少一次送达，当事务提交后才标注元素可清除
                try {
                    jmsClient.commitTransaction();
                    return idx;
                } catch (JMSException e) {
                    log.error("[NorthMessaging]JMS提交事务异常：", e);
                }
            }

            // JMS链接异常：重新建立链接
            log.warn("[NorthMessaging]JMS链接发现异常，关闭链接");
            jmsClient.close();
            jmsClient = null;
            sleepAMoment(SLEEP_SHORT_TIME);
            return srcIdx;
        }

        private void sleepAMoment(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                log.error("[NorthMessaging]等待被打断：", e);
            }
        }

    }
}
