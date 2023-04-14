package org.jetlinks.community.network.coap.server.lwm2m.impl;

import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityStore;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MServer;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.LwM2MAuthenticationRequest;
import org.jetlinks.core.message.codec.lwm2m.LwM2MDownlinkMessage;
import org.jetlinks.core.message.codec.lwm2m.LwM2MUplinkMessage;
import org.jetlinks.core.message.codec.lwm2m.SimpleLwM2MUplinkMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

/**
 * @author v-lizy8
 * @date 2023/3/27
 */
@Slf4j
public class LeShanLwM2MServer implements LwM2MServer {

    private final String    id;

    private LeshanServer    server;

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bindAddress;

    private LwM2MAuthorizer authorizer;

    private Lwm2mObservationListener    observationListener;

    private Flux<Tuple3<LwM2MDownlinkMessage, LwM2MUplinkMessage, Exception>> replyFlux;

    private FluxSink<Tuple3<LwM2MDownlinkMessage, LwM2MUplinkMessage, Exception>> replyFluxSink;

    public LeShanLwM2MServer(String id) {
        this.id = id;

        this.replyFlux = Flux.create(sink -> this.replyFluxSink = sink);
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
    public Flux<LwM2MUplinkMessage> handleReply() {
        return null;
    }

    @Override
    public Mono<Void> send(LwM2MDownlinkMessage message) {
        final Registration registration = server.getRegistrationService().getById(message.getRegistrationId());
        if (registration == null) {
            return Mono.empty();
        }

        String payload = Hex.encodeHexString(message.getPayload().array());
        final ExecuteRequest request = new ExecuteRequest(message.getObjectAndResource().getPath(), payload);

        return Mono.fromCallable(() -> {
            ResponseCallback<ExecuteResponse>   rspCallback = (rsp) -> {
                SimpleLwM2MUplinkMessage uplinkMessage = new SimpleLwM2MUplinkMessage();
                Response coapResponse = (Response) rsp.getCoapResponse();
                uplinkMessage.setMessageId(coapResponse.getMID());
                uplinkMessage.setRegistrationId(message.getRegistrationId());
                uplinkMessage.setResource(message.getObjectAndResource());
                uplinkMessage.setCode(rsp.getCode().getCode());
                uplinkMessage.setPayload(Unpooled.wrappedBuffer(coapResponse.getPayload()));

                replyFluxSink.next(Tuples.of(message, uplinkMessage, null));
            };
            ErrorCallback errorCallback = (e) -> {
                replyFluxSink.next(Tuples.of(message, null, e));
            };

            server.send(registration, request, 5000, rspCallback, errorCallback);

            return null;
        }).then();
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
        if (server == null) return ;

        server.destroy();
        server = null;
        authorizer = null;
        observationListener = null;

        log.debug("LwM2M server [{}] closed", id);
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
