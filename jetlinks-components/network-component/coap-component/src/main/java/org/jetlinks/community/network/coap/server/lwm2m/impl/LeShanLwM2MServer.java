package org.jetlinks.community.network.coap.server.lwm2m.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.security.SecurityStore;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MServer;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.LwM2MAuthenticationRequest;
import org.jetlinks.core.message.codec.lwm2m.LwM2MDownlinkMessage;
import org.jetlinks.core.message.codec.lwm2m.LwM2MUplinkMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

/**
 * @author v-lizy8
 * @date 2023/3/27
 */
@Slf4j
public class LeShanLwM2MServer implements LwM2MServer {

    private final String  id;

    private LeshanServer    server;

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bindAddress;

    private LwM2MAuthorizer authorizer;

    private Lwm2mObservationListener    observationListener;

    public LeShanLwM2MServer(String id) {
        this.id = id;
    }

    public void setLeShanServer(LeshanServer server) {
        this.server = server;
    }

    @Override
    public Flux<LwM2MAuthenticationRequest> handleAuth() {
        return authorizer.handleAuthentication();
    }

    @Override
    public Flux<LwM2MUplinkMessage> handleObservation() {
        return observationListener.handleObservation();
    }

    @Override
    public Mono<Void> send(LwM2MDownlinkMessage message) {
        return null;
    }

    @Override
    public void startUp() {
        server.start();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.LWM2M_SERVER;
    }

    @Override
    public void shutdown() {
        if (server != null) {
            server.destroy();
            server = null;
            authorizer = null;
            observationListener = null;

            log.debug("LwM2M server [{}] closed", id);
        }
    }

    @Override
    public boolean isAlive() {
        return server != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }

    @Nullable
    @Override
    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    public LwM2MAuthorizer buildAndBindAuthorizer(SecurityStore store, DeviceRegistry deviceRegistry) {
        this.authorizer = new LwM2MAuthorizer(store, deviceRegistry);
        return authorizer;
    }

    public Lwm2mRegistrationListener buildAndBindRegistrationListener() {
        return new Lwm2mRegistrationListener();
    }

    public Lwm2mPresenceListener buildAndBindPresenceListener() {
        return new Lwm2mPresenceListener();
    }

    public Lwm2mObservationListener buildAndBindObservationListener() {
        this.observationListener =  new Lwm2mObservationListener();
        return this.observationListener;
    }
}
