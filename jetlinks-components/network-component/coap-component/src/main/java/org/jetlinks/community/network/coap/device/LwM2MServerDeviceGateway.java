package org.jetlinks.community.network.coap.device;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.gateway.monitor.MonitorSupportDeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.CoapMessage;
import org.jetlinks.community.network.coap.server.coap.CoapServer;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MServer;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.DeviceGatewayContext;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * LwM2M协议网关
 *
 * @author tenyks
 * @version 2.0
 */
@Slf4j
class LwM2MServerDeviceGateway extends AbstractDeviceGateway {

    final LwM2MServer server;

    Mono<ProtocolSupport> protocol;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

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
        this.sessionManager = sessionManager;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
    }

    public Mono<ProtocolSupport> getProtocol() {
        return protocol;
    }

    public Transport getTransport() {
        return DefaultTransport.LwM2M;
    }

    public NetworkType getNetworkType() {
        return DefaultNetworkType.TCP_SERVER;
    }


    private void doStart() {

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
