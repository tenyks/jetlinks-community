package org.jetlinks.community.network.coap.device;

import groovy.lang.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MServer;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.AuthenticationResponse;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.LwM2MAuthenticationRequest;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * LwM2M协议网关
 *
 * @author tenyks
 * @version 2.0
 */
@Slf4j
public class LwM2MServerDeviceGateway extends AbstractDeviceGateway {

    final LwM2MServer server;

    private final Mono<ProtocolSupport> protocol;

    private final DeviceRegistry registry;

    private final LongAdder counter = new LongAdder();

    private final AtomicBoolean started = new AtomicBoolean();

    private Disposable disposable;

    private final DeviceGatewayHelper helper;


    public LwM2MServerDeviceGateway(String id,
                                    Mono<ProtocolSupport> protocol,
                                    DeviceRegistry deviceRegistry,
                                    DecodedClientMessageHandler clientMessageHandler,
                                    DeviceSessionManager sessionManager,
                                    LwM2MServer server) {
        super(id);
        this.protocol = protocol;
        this.registry = deviceRegistry;
        this.server = server;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
    }

    public Mono<ProtocolSupport> getProtocol() {
        return protocol;
    }

    public Transport getTransport() {
        return DefaultTransport.LwM2M;
    }

    public NetworkType getNetworkType() {
        return DefaultNetworkType.LWM2M_SERVER;
    }

    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }

        Disposable forAuthDisposable = server
            .handleAuthentication()
            .publishOn(Schedulers.parallel())
            .flatMap(authReq -> handleAuthRequest(authReq)
                    .onErrorResume(err -> {
                        log.error("handle tcp client[{}] error", authReq, err);
                        return Mono.empty();
                    })
                , Integer.MAX_VALUE)
            .contextWrite(ReactiveLogger.start("network", server.getId()))
            .subscribe(
                ignore -> {},
                error -> log.error(error.getMessage(), error)
            );

        Disposable forObservationDisposable = server
            .handleObservation()
            .publishOn(Schedulers.boundedElastic())
            .flatMap(message -> {
                return message;
            }, Integer.MAX_VALUE)
            .subscribe(ignore -> {
            }, error -> log.error(error.getMessage(), error));
    }

    private Mono<Tuple2<DeviceOperator, AuthenticationResponse>>    handleAuthRequest(LwM2MAuthenticationRequest authReq) {
        return Mono.empty();
    }

    @Override
    protected Mono<Void> doStartup() {
        return Mono.fromRunnable(this::doStart);
    }

    @Override
    protected Mono<Void> doShutdown() {
        return Mono.fromRunnable(() -> {
            if (null != disposable) {
                disposable.dispose();
                disposable = null;
            }
        });
    }

}
