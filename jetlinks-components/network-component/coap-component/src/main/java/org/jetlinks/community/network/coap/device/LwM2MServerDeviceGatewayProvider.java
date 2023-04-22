package org.jetlinks.community.network.coap.device;

import org.jetlinks.community.network.coap.server.lwm2m.LwM2MServer;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.server.coap.CoapServer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

/**
 *
 *  @author tenyks
 *  @version 1.0
 */
@Component
public class LwM2MServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(LwM2MServerDeviceGatewayProvider.class);

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;


    public LwM2MServerDeviceGatewayProvider(NetworkManager networkManager,
                                            DeviceRegistry registry,
                                            DeviceSessionManager sessionManager,
                                            DecodedClientMessageHandler messageHandler,
                                            ProtocolSupports protocolSupports) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.messageHandler = messageHandler;
        this.protocolSupports = protocolSupports;
    }

    @Override
    public String getId() {
        return "lwm2m-server-gateway";
    }

    @Override
    public String getName() {
        return "LwM2M直连接入";
    }

    public NetworkType getNetworkType() {
        return DefaultNetworkType.LWM2M_SERVER;
    }

    public Transport getTransport() {
        return DefaultTransport.CoAP;
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        log.info("[LwM2MGateway]即将创建协议网关..., properties={}", properties);

        return networkManager
            .<LwM2MServer>getNetwork(getNetworkType(), properties.getChannelId())
            .map(server -> {
                String protocol = properties.getProtocol();

                Assert.hasText(protocol, "protocol can not be empty");

                ProtocolSupport ps = protocolSupports.getProtocol(protocol).block(Duration.ofSeconds(5));

                return new LwM2MServerDeviceGateway(
                    properties.getId(),
                    Mono.just(ps),
                    registry,
                    messageHandler,
                    sessionManager,
                    server
                );
            });
    }

    @Override
    public Mono<? extends DeviceGateway> reloadDeviceGateway(DeviceGateway gateway,
                                                             DeviceGatewayProperties properties) {
        log.info("[LwM2MGateway]即将创建协议网关..., properties={}", properties);

        LwM2MServerDeviceGateway deviceGateway = ((LwM2MServerDeviceGateway) gateway);
        //网络组件发生变化
        if (!Objects.equals(deviceGateway.getId(), properties.getChannelId())) {
            return gateway
                .shutdown()
                .then(this.createDeviceGateway(properties))
                .flatMap(newer -> newer.startup().thenReturn(newer));
        }

        //更新协议
        String protocol = properties.getProtocol();
        deviceGateway.setCustProtocol(Mono.defer(() -> protocolSupports.getProtocol(protocol)));

        return Mono.just(gateway);
    }
}
