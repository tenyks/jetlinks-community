package org.jetlinks.community.network.coap.server.lwm2m.impl;

import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.jetlinks.community.network.coap.device.DefaultLwM2MPresenceEvent;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MPresenceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Observable;

/**
 * Lwm2m协议设备休眠和唤醒状态
 */
public class Lwm2mPresenceListener implements PresenceListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private FluxSink<LwM2MPresenceEvent> fluxSink;

    private final Flux<LwM2MPresenceEvent> flux;

    public Lwm2mPresenceListener() {
        flux = Flux.create(sink -> this.fluxSink = sink);
    }

    public Flux<LwM2MPresenceEvent> handlePresenceEvent() {
        return flux;
    }

    @Override
    public void onSleeping(Registration registration) {
        if (logger.isDebugEnabled()) {
            logger.debug("[LwM2m]Device({}) is on sleeping", registration.getEndpoint());
        }

        fluxSink.next(new DefaultLwM2MPresenceEvent(registration, false));
    }

    @Override
    public void onAwake(Registration registration) {
        if (logger.isDebugEnabled()) {
            logger.debug("[LwM2m]Device({}) is on awake", registration.getEndpoint());
        }

        fluxSink.next(new DefaultLwM2MPresenceEvent(registration, false));
    }
}
