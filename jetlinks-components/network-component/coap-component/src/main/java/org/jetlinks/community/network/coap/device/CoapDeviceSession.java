package org.jetlinks.community.network.coap.device;

import lombok.Setter;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.coap.server.coap.CoapClient;
import org.jetlinks.community.network.coap.server.coap.CoapExchange;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

/**
 * COAP 设备会话
 *
 * @author zhouhao
 * @since 1.0
 */
class CoapDeviceSession implements DeviceSession {

    private final DeviceOperator    operator;

    private final InetSocketAddress address;

    @Setter
    private CoapExchange    exchange;

    private long lastPingTime = System.currentTimeMillis();

    //默认永不超时
    private long keepAliveTimeOutMs = -1;

    public CoapDeviceSession(DeviceOperator deviceOperator, InetSocketAddress address) {
        this.operator = deviceOperator;
        this.address = address;
    }

    public CoapDeviceSession(DeviceOperator deviceOperator, CoapClient client, Transport transport,
                             DeviceGatewayMonitor monitor) {
        this.operator = deviceOperator;
        this.address = client.getRemoteAddress();
    }

    @Override
    public String getId() {
        return operator.getDeviceId();
    }

    @Override
    public String getDeviceId() {
        return operator.getDeviceId();
    }

    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return operator;
    }

    @Override
    public long lastPingTime() {
        return lastPingTime;
    }

    @Override
    public long connectTime() {
        return lastPingTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Mono.just(Boolean.FALSE);
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.HTTP;
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.ofNullable(address);
    }

    @Override
    public void close() {

    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeOutMs = timeout.toMillis();
    }

    @Override
    public void ping() {
        //ISSUE 应该不用发送，即使发送了设备也有可能休眠接收不到消息

        lastPingTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAlive() {
        return keepAliveTimeOutMs <= 0
            || System.currentTimeMillis() - lastPingTime < keepAliveTimeOutMs;
    }

    public void setClient(CoapClient client) {

    }

    @Override
    public void onClose(Runnable call) {

    }
}
