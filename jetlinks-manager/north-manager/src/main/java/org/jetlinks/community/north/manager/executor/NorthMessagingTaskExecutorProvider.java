package org.jetlinks.community.north.manager.executor;

import com.alibaba.fastjson.JSONObject;
import org.apache.activemq.AlreadyClosedException;
import org.apache.activemq.ConnectionClosedException;
import org.apache.commons.lang3.StringUtils;
import org.jetlinks.community.north.manager.message.NorthMessage;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.jms.ActiveMQClientImpl;
import org.jetlinks.core.jms.JMSClient;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
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
@Component
public class NorthMessagingTaskExecutorProvider implements TaskExecutorProvider {

    private static final Logger log = LoggerFactory.getLogger(NorthMessagingTaskExecutorProvider.class);

    private static final List<String> SUBSCRIBED_TOPICS = new ArrayList<>();
    private static final String DST_NORTH_TOPIC = "/iot/northMessage";
    private static final long   SLEEP_SHORT_TIME = 1000;
    private static final long   SLEEP_LONG_TIME = 30000;

    static {
        SUBSCRIBED_TOPICS.add("/message/property/report");
        SUBSCRIBED_TOPICS.add("/message/property/read/reply");
        SUBSCRIBED_TOPICS.add("/message/property/write/reply");
        SUBSCRIBED_TOPICS.add("/message/function/reply");
        SUBSCRIBED_TOPICS.add("/register");
        SUBSCRIBED_TOPICS.add("/unregister");
        SUBSCRIBED_TOPICS.add("/message/unknown");
        SUBSCRIBED_TOPICS.add("/message/log");
        SUBSCRIBED_TOPICS.add("/message/direct");
        SUBSCRIBED_TOPICS.add("/online");
        SUBSCRIBED_TOPICS.add("/offline");
    }

    private final EventBus eventBus;

    private final String    jmsBrokerUrl;

    public NorthMessagingTaskExecutorProvider(EventBus eventBus, @Value("${system.jms.jmsBrokerUrl:}") String jmsBrokerUrl) {
        this.eventBus = eventBus;
        this.jmsBrokerUrl = jmsBrokerUrl;
    }

    @Override
    public String getExecutor() {
        return "north-messaging";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        if (StringUtils.isBlank(jmsBrokerUrl)) {
            log.warn("[北向推送]没有配置北向消息JMS Broker地址， 不推送北向消息");
            return null;
        }

        return null;
    }

    class NorthMessagingTaskExecutor extends AbstractTaskExecutor implements Runnable, Disposable {

        private JMSClient   jmsClient;

        private BlockingQueue<TopicPayload> queue;

        private AtomicBoolean stop = new AtomicBoolean(false);

        public NorthMessagingTaskExecutor(ExecutionContext context) {
            super(context);

            this.queue = new LinkedBlockingDeque<>();
        }

        @Override
        public String getName() {
            return "NorthMessaging";
        }

        @Override
        protected Disposable doStart() {
            String subId = "north-messaging:".concat(context.getInstanceId()).concat(":").concat(context.getJob().getNodeId());
            Subscription subscription = Subscription.builder().subscriberId(subId).topics(SUBSCRIBED_TOPICS).local().build();
            Disposable disposable1 = eventBus.subscribe(subscription, this::handleMessage);

            Thread pushingThread = new Thread(this, "north-pushing-thread");

            pushingThread.start();

            return Disposables.composite(disposable1, this);
        }

        @Override
        public void dispose() {
            stop.set(true);
        }

        private Mono<Void> handleMessage(TopicPayload payload) {
            return Mono.defer(() -> {
                queue.add(payload);
                return Mono.empty();
            });
        }

        @Override
        public void run() {
            List<TopicPayload> buf = new ArrayList<>(110);

            int idx = 0;
            log.warn("[NorthMessaging]开始北向消息推送...");
            while (!stop.get()) {
                if (buf.isEmpty()) {
                    // 使用平衡高吞吐和高响应延时的方式读取queue
                    if (queue.isEmpty()) {
                        try {
                            TopicPayload tp = queue.poll(30, TimeUnit.SECONDS);
                            if(tp != null) {
                                buf.add(tp);
                            } else {
                                log.info("[NorthMessaging]无消息");
                            }
                        } catch (InterruptedException e) {
                            log.info("[NorthMessaging]等待消息被中断：", e);
                            continue;
                        }
                    }

                    queue.drainTo(buf, Math.min(queue.size(), 100));
                }

                idx = pushingBatchMessage(buf, idx);
                if (idx >= buf.size()) {
                    log.info("[NorthMessaging]推送了{}条消息", buf.size());

                    idx = 0;
                    buf.clear();
                }
            }

            log.warn("[NorthMessaging]终止北向消息推送。");
        }

        private int pushingBatchMessage(List<TopicPayload> payloadList, int srcIdx) {
            if (payloadList.isEmpty()) return srcIdx;

            if (jmsClient == null) {
                try {
                    jmsClient = new ActiveMQClientImpl("iot-north-messaging", jmsBrokerUrl);
                } catch (JMSException e) {
                    log.error("[NorthMessaging]JMS建立链接失败：url={}", jmsBrokerUrl, e);
                    sleepAMoment(SLEEP_LONG_TIME);
                    return srcIdx;
                }
            }

            boolean reconnectClient = false;
            int idx = srcIdx;
            for (; idx < payloadList.size(); idx++) {
                TopicPayload tp = payloadList.get(idx);

                NorthMessage northMsg = NorthMessage.fromTopicPayload(tp);
                String payloadStr = JSONObject.toJSONString(northMsg);
                try {
                    jmsClient.send(DST_NORTH_TOPIC, payloadStr);
                    log.debug("[NorthMessaging]消息已发送：{}", payloadStr);
                } catch (ConnectionClosedException | AlreadyClosedException e) {
                    reconnectClient = true;
                    log.warn("[NorthMessaging]JMS链接发现异常：", e);
                    break;
                } catch (JMSException e) {
                    log.error("[NorthMessaging]忽略消息：{}", payloadStr);
                    sleepAMoment(SLEEP_SHORT_TIME);
                    break;
                }
            }

            if (!reconnectClient) {
                // 保证消息最少一次送达，当事务提交后才清除元素
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

        @Override
        public void reload() {
            if (this.disposable != null) {
                this.disposable.dispose();
            }
            start();
        }

    }
}
