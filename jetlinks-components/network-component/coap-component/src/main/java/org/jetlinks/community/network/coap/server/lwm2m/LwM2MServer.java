package org.jetlinks.community.network.coap.server.lwm2m;

import org.jetlinks.community.network.ServerNetwork;
import org.jetlinks.core.device.LwM2MAuthenticationRequest;
import org.jetlinks.core.message.codec.lwm2m.LwM2MDownlinkMessage;
import org.jetlinks.core.message.codec.lwm2m.LwM2MExchangeMessage;
import org.jetlinks.core.message.codec.lwm2m.LwM2MUplinkMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * LwM2M服务端
 *
 * @author dumas.lee
 * @version 2.0
 * @since 2.0
 */
public interface LwM2MServer extends ServerNetwork {

    /**
     * 监听所有请求
     *
     * @return Observation回复消息
     */
    Flux<LwM2MUplinkMessage>    handleObservation();

    /**
     * 监听Reply消息，如：指令回复、读属性的回复等
     * @return  Reply消息
     */
    Flux<LwM2MExchangeMessage>    handleReply();

    /**
     * @return  监听设备认证请求
     */
    Flux<LwM2MAuthenticationRequest> handleAuthentication();

    /**
     * @return  监听设备上下线事件
     */
    Flux<LwM2MRegistrationEvent>    handleRegistrationEvent();

    /**
     * @return  监听设备睡眠/唤醒事件
     */
    Flux<LwM2MPresenceEvent>        handlePresenceEvent();

    /**
     * 发送消息到客户端
     *
     * @param message MQTT消息
     * @return 异步推送结果
     */
    Mono<Boolean> send(LwM2MDownlinkMessage message);

    /**
     * 启动服务端
     */
    void startUp();

    /**
     * 停止服务端
     */
    void shutdown();
}
