package org.jetlinks.community.network.coap.gateway.device;

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
import org.jetlinks.community.network.coap.server.CoapServer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 *
 *  @author tenyks
 *  @version 1.0
 */
@Component
public class CoapServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;


    public CoapServerDeviceGatewayProvider(NetworkManager networkManager,
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
        return "coap-server-gateway";
    }

    @Override
    public String getName() {
        return "Coap透传接入";
    }

    public NetworkType getNetworkType() {
        return DefaultNetworkType.TCP_SERVER;
    }

    public Transport getTransport() {
        return DefaultTransport.TCP;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<CoapServer>getNetwork(getNetworkType(), properties.getChannelId())
            .map(tcpServer -> {
                String protocol = properties.getProtocol();

                Assert.hasText(protocol, "protocol can not be empty");

                return new CoapServerDeviceGateway(
                    properties.getId(),
                    Mono.defer(() -> protocolSupports.getProtocol(protocol)),
                    registry,
                    messageHandler,
                    sessionManager,
                    tcpServer
                );
            });
    }

    @Override
    public Mono<? extends DeviceGateway> reloadDeviceGateway(DeviceGateway gateway,
                                                             DeviceGatewayProperties properties) {
        CoapServerDeviceGateway deviceGateway = ((CoapServerDeviceGateway) gateway);
        //网络组件发生变化
        if (!Objects.equals(deviceGateway.tcpServer.getId(), properties.getChannelId())) {
            return gateway
                .shutdown()
                .then(this.createDeviceGateway(properties))
                .flatMap(newer -> newer.startup().thenReturn(newer));
        }
        //更新协议
        String protocol = properties.getProtocol();
        deviceGateway.protocol = Mono.defer(() -> protocolSupports.getProtocol(protocol));

        return Mono.just(gateway);
    }
}
