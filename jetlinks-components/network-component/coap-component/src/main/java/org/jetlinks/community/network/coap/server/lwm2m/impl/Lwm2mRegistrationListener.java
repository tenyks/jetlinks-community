package org.jetlinks.community.network.coap.server.lwm2m.impl;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.jetlinks.community.network.coap.device.DefaultLwM2MRegistrationEvent;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MRegistrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Collection;

/**
 * 设备上下线事件监听器
 *
 * @author v-lizy8
 * @date 2023/3/28
 */
public class Lwm2mRegistrationListener implements RegistrationListener {

    private final Logger log = LoggerFactory.getLogger(Lwm2mRegistrationListener.class);

    private Flux<LwM2MRegistrationEvent>    regEventFlux;

    private FluxSink<LwM2MRegistrationEvent> regEventFluxSink;

    public Lwm2mRegistrationListener() {
        this.regEventFlux = Flux.create(sink -> this.regEventFluxSink = sink);
    }

    public Flux<LwM2MRegistrationEvent> handleRegistrationEvent() {
        return regEventFlux;
    }

    @Override
    public void registered(Registration registration, Registration previousReg, Collection<Observation> collection) {
        // 发送设备上线通知开始
        log.debug("[LwM2M]online message: reg={}", registration);

        String endpoint = Lwm2mRegistrationIdProvider.normalizeEndpoint(registration.getEndpoint());
        LwM2MRegistrationEvent event = DefaultLwM2MRegistrationEvent.ofOnline(endpoint, registration.getId());
        regEventFluxSink.next(event);
    }

    @Override
    public void updated(RegistrationUpdate update, Registration updatedRegistration, Registration previousRegistration) {
        log.debug("[LwM2M]Registration updated: reg={}", updatedRegistration);

        String endpoint = Lwm2mRegistrationIdProvider.normalizeEndpoint(updatedRegistration.getEndpoint());
        LwM2MRegistrationEvent event = DefaultLwM2MRegistrationEvent.ofUpdate(endpoint, updatedRegistration.getId(),
                                                (previousRegistration != null ? previousRegistration.getId() : null));
        regEventFluxSink.next(event);
    }

    @Override
    public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                             Registration newReg) {
        // 设备下线通知
        log.debug("[LwM2M]offline message: reg={}", registration);

        String endpoint = Lwm2mRegistrationIdProvider.normalizeEndpoint(registration.getEndpoint());
        LwM2MRegistrationEvent event = DefaultLwM2MRegistrationEvent.ofOffline(endpoint, registration.getId());
        regEventFluxSink.next(event);
    }
}
