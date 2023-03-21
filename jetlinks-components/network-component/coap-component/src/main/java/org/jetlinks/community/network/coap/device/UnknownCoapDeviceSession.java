package org.jetlinks.community.network.coap.device;

import lombok.Getter;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.coap.server.coap.CoapClient;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.network.coap.CoapMessage;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

/**
 *
 *  @author tenyks
 *  @version 1.0
 */
class UnknownCoapDeviceSession implements DeviceSession {

    @Getter
    private final String id;

    private final CoapClient client;

    @Getter
    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    private final long connectTime = System.currentTimeMillis();

    private final DeviceGatewayMonitor monitor;
    private Duration keepAliveTimeout;

    UnknownCoapDeviceSession(String id, CoapClient client, Transport transport, DeviceGatewayMonitor monitor) {
        this.id = id;
        this.client = client;
        this.transport = transport;
        this.monitor = monitor;
    }

    @Override
    public String getDeviceId() {
        return "unknown";
    }

    @Override
    public DeviceOperator getOperator() {
        return null;
    }

    @Override
    public long lastPingTime() {
        return lastPingTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return client.send(new CoapMessage(encodedMessage.getPayload()))
                     .doOnSuccess(ignore -> monitor.sentMessage());
    }

    @Override
    public void close() {
        client.shutdown();
    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
        client.keepAlive();
    }

    @Override
    public boolean isAlive() {
        return client.isAlive();
    }

    @Override
    public void onClose(Runnable call) {
        client.onDisconnect(call);
    }

    @Override
    public void setKeepAliveTimeout(Duration keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.of(client.getRemoteAddress());
    }
}
