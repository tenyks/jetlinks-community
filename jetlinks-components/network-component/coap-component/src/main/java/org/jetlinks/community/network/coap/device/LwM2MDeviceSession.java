package org.jetlinks.community.network.coap.device;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

/**
 * @author v-lizy8
 * @date 2023/3/27
 */
public class LwM2MDeviceSession implements DeviceSession {
    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getDeviceId() {
        return null;
    }

    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return null;
    }

    @Override
    public long lastPingTime() {
        return 0;
    }

    @Override
    public long connectTime() {
        return 0;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return null;
    }

    @Override
    public Transport getTransport() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void ping() {

    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public void onClose(Runnable call) {

    }
}
