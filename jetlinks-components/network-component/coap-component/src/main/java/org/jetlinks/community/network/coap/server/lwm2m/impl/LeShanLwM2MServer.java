package org.jetlinks.community.network.coap.server.lwm2m.impl;

import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.exception.*;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.SecurityStore;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MRegistrationEvent;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MServer;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.LwM2MAuthenticationRequest;
import org.jetlinks.core.message.codec.lwm2m.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

/**
 * 基于Eclipse LeShan的LwM2M协议服务器
 * @author v-lizy8
 * @date 2023/3/27
 * @version 1.0
 * @since 2.0
 */
@Slf4j
public class LeShanLwM2MServer implements LwM2MServer {

    private final String    id;

    private LeshanServer    server;

    /**
     * 响应超时的时长，单位：毫秒
     */
    private long            responseWaitTime;

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bindAddress;

    private LwM2MAuthorizer             authorizer;

    private Lwm2mObservationListener    observationListener;

    private Lwm2mRegistrationListener   registrationListener;

    private Flux<LwM2MExchangeMessage>  replyFlux;

    private FluxSink<LwM2MExchangeMessage> replyFluxSink;

    public LeShanLwM2MServer(String id, long responseWaitTime) {
        this.id = id;
        this.responseWaitTime = responseWaitTime;

        this.replyFlux = Flux.create(sink -> this.replyFluxSink = sink);
    }

    public void setLeShanServer(LeshanServer server) {
        this.server = server;
    }

    @Override
    public Flux<LwM2MAuthenticationRequest> handleAuthentication() {
        return authorizer.handleAuthentication();
    }

    @Override
    public Flux<LwM2MUplinkMessage> handleObservation() {
        return observationListener.handleObservation();
    }

    @Override
    public Flux<LwM2MExchangeMessage> handleReply() {
        return replyFlux;
    }

    @Override
    public Flux<LwM2MRegistrationEvent> handleRegistrationEvent() {
        return registrationListener.handleRegistrationEvent();
    }

    @Override
    public Mono<Boolean> send(LwM2MDownlinkMessage message) {
        final Registration registration = server.getRegistrationService().getById(message.getRegistrationId());
        if (registration == null) {
            return Mono.error(new IllegalStateException("会话已过期或设备已离线[0x05LSLMS9381]"));
        }

        LwM2MOperation operation = message.getRequestOperation();
        LwM2MResource resource = LwM2MResource.parse(message.getPath());

        if (operation.equals(LwM2MOperation.Observe)) {
            return Mono.fromCallable(() -> {
                final ObserveRequest request = new ObserveRequest(message.getPath());

                server.send(registration, request, responseWaitTime,
                            buildResponseCallback(message), buildErrorCallback(message));

                return true;
            });
        }
        if(operation.equals(LwM2MOperation.Write)){
            return Mono.fromCallable(() -> {
                final WriteRequest request = new WriteRequest(
                    ContentFormat.JSON, resource.getObjectId(), resource.getObjectInstanceId(), resource.getResourceId(),
                    message.payloadAsString()
                );

                server.send(registration, request, responseWaitTime,
                    buildResponseCallback(message), buildErrorCallback(message));

                return true;
            });
        }
        if (operation.equals(LwM2MOperation.Execute)) {
            return Mono.fromCallable(() -> {
                final ExecuteRequest request = new ExecuteRequest(
                    resource.getObjectId(), resource.getObjectInstanceId(), resource.getResourceId(),
                    message.payloadAsString()
                );
                server.send(registration, request, responseWaitTime,
                    buildResponseCallback(message), buildErrorCallback(message));

                return true;
            });
        }

        return Mono.just(false);
    }

    private <T extends LwM2mResponse> ResponseCallback<T> buildResponseCallback(LwM2MDownlinkMessage message) {
        return new ResponseCallback<T>() {
            @Override
            public void onResponse(T response) {
                SimpleLwM2MUplinkMessage replyMessage = new SimpleLwM2MUplinkMessage();
                Response coapResponse = (Response) response.getCoapResponse();
                replyMessage.setMessageId(coapResponse.getMID());
                replyMessage.setRegistrationId(message.getRegistrationId());
                replyMessage.setPath(message.getPath());
                replyMessage.setResponseCode(response.getCode().getCode());
                replyMessage.setPayload(Unpooled.wrappedBuffer(coapResponse.getPayload()));

                replyFluxSink.next(SimpleLwM2MExchangeMessage.ofSuccess(message, replyMessage));
            }
        };
    }

    private ErrorCallback    buildErrorCallback(LwM2MDownlinkMessage message) {
        return new ErrorCallback() {
            @Override
            public void onError(Exception e) {
                String rstCode;
                String rstMsg = e.getMessage();

                if (e instanceof RequestRejectedException) {
                    rstCode = LwM2MExchangeMessage.RC_REJECTED_BY_PEER;
                } else if (e instanceof RequestCanceledException) {
                    rstCode = LwM2MExchangeMessage.RC_CANCELLED;
                } else if (e instanceof SendFailedException) {
                    rstCode = LwM2MExchangeMessage.RC_SEND_FAIL;
                } else if (e instanceof InvalidResponseException) {
                    rstCode = LwM2MExchangeMessage.RC_UNKNOWN_RESPONSE;
                } else if (e instanceof ClientSleepingException) {
                    rstCode = LwM2MExchangeMessage.RC_CLIENT_SLEEP;
                } else if (e instanceof TimeoutException) {
                    rstCode = LwM2MExchangeMessage.RC_TIMEOUT;
                } else {
                    rstCode = "RC_NOT_CLASSIFIED";
                }

                replyFluxSink.next(SimpleLwM2MExchangeMessage.ofFail(message, rstCode, rstMsg, e));
            }
        };
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
        if (server == null) {
            log.warn("LwM2M server [{}] has been closed", id);
            return ;
        }

        server.destroy();

        server = null;
        authorizer = null;
        observationListener = null;
        registrationListener = null;

        log.warn("LwM2M server [{}] closed", id);
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

    public Authorizer buildAndBindAuthorizer(SecurityStore store, DeviceRegistry deviceRegistry) {
        this.authorizer = new LwM2MAuthorizer(store, deviceRegistry);
        return this.authorizer;
    }

    public RegistrationListener buildAndBindRegistrationListener() {
        this.registrationListener = new Lwm2mRegistrationListener();
        return this.registrationListener;
    }

    public PresenceListener buildAndBindPresenceListener() {
        return new Lwm2mPresenceListener();
    }

    public ObservationListener buildAndBindObservationListener() {
        this.observationListener =  new Lwm2mObservationListener();
        return this.observationListener;
    }
}
