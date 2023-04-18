package org.jetlinks.community.network.coap.server.lwm2m.impl;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityStore;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.LwM2MAuthenticationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.validation.constraints.Null;
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

    private final DeviceRegistry deviceRegistry;

    private FluxSink<LwM2MAuthenticationRequest> fluxSink;

    private Flux<LwM2MAuthenticationRequest> flux;

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
    }

    public Flux<LwM2MAuthenticationRequest>     handleAuthentication() {
        return this.flux;
    }

    @Override
    public Registration isAuthorized(UplinkRequest<?> request, Registration registration, Identity senderIdentity) {
        _LwM2MAuthenticationRequest authReq = new _LwM2MAuthenticationRequest(request, registration);
        fluxSink.next(authReq);

        try {
            return authReq.getResultFuture().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("LwM2M设备认证超时：req={}, reg={}", request, registration, e);
            return null;
        }
    }

    public static class _LwM2MAuthenticationRequest extends LwM2MAuthenticationRequest {

        private final CompletableFuture<Registration> resultFuture;

        private Registration      registration;

        public _LwM2MAuthenticationRequest(UplinkRequest<?> request, Registration registration) {
            super(registration.getEndpoint());

            this.registration = registration;
            this.resultFuture = new CompletableFuture<>();
            if (registration.getAddress() != null) {
                this.setClientAddress(registration.getAddress().toString());
            }
        }

        @Override
        public void complete(boolean accepted, @Null String message) {
            if (accepted) {
                this.resultFuture.complete(registration);
            } else {
                this.resultFuture.complete(null);
            }
        }

        @Override
        public Registration getRegistration() {
            return registration;
        }

        public CompletableFuture<Registration> getResultFuture() {
            return resultFuture;
        }
    }

}
