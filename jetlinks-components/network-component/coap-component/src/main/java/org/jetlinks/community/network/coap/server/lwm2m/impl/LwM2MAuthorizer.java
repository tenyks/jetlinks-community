package org.jetlinks.community.network.coap.server.lwm2m.impl;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityStore;
import org.jetlinks.core.device.DeviceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * LwM2M协议授权认证器
 *
 * @author v-lizy8
 * @date 2023/3/27
 */
public class LwM2MAuthorizer extends DefaultAuthorizer {

    private static final Logger log = LoggerFactory.getLogger(LwM2MAuthorizer.class);

    private DeviceRegistry deviceRegistry;

    private FluxSink<RegistrationDemand> fluxSink;

    private Flux<RegistrationDemand> flux;

    public LwM2MAuthorizer(SecurityStore store, DeviceRegistry deviceRegistry) {
        super(store);
        this.deviceRegistry = deviceRegistry;

        init();
    }

    public LwM2MAuthorizer(SecurityStore store, SecurityChecker checker, DeviceRegistry deviceRegistry) {
        super(store, checker);
        this.deviceRegistry = deviceRegistry;

        init();
    }

    private void init() {
        this.flux = Flux.create(emitter -> this.fluxSink = emitter);
        this.flux.subscribe(this::_isAuthorized);
    }

    @Override
    public Registration isAuthorized(UplinkRequest<?> request, Registration registration, Identity senderIdentity) {
        // 父方法认证
//        registration = super.isAuthorized(request, registration, senderIdentity);
//        if (registration != null) {
//            return registration;
//        }

        RegistrationDemand demand = new RegistrationDemand(request, registration, senderIdentity);
        fluxSink.next(demand);

        try {
            return demand.getResultFuture().get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("[Registration]", e);
            return null;
        }
    }



    private Mono<RegistrationDemand> _isAuthorized(RegistrationDemand demand) {
        Registration registration = demand.getRegistration();

        // 设备imei号码认证
        String deviceId = registration.getEndpoint();
        return deviceRegistry.getDevice(deviceId)
            .map(deviceOperator -> {
                demand.resultFuture.complete(registration);
                return demand;
            })
            .switchIfEmpty(d -> {
                demand.resultFuture.complete(null);
                return demand;
            });
    }

    private class RegistrationDemand {
        private UplinkRequest<?> request;

        private Registration registration;

        private Identity senderIdentity;

        private CompletableFuture<Registration> resultFuture;

        public RegistrationDemand(UplinkRequest<?> request, Registration registration, Identity senderIdentity) {
            this.request = request;
            this.registration = registration;
            this.senderIdentity = senderIdentity;
            this.resultFuture = new CompletableFuture<>();
        }

        public UplinkRequest<?> getRequest() {
            return request;
        }

        public Registration getRegistration() {
            return registration;
        }

        public Identity getSenderIdentity() {
            return senderIdentity;
        }

        public CompletableFuture<Registration> getResultFuture() {
            return resultFuture;
        }
    }
}
