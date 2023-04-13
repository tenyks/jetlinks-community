package org.jetlinks.community.network.coap.server.lwm2m.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MServer;
import org.jetlinks.core.device.LwM2MAuthenticationRequest;
import org.jetlinks.core.message.codec.lwm2m.LwM2MDownlinkMessage;
import org.jetlinks.core.message.codec.lwm2m.LwM2MExchangeMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

/**
 * @author v-lizy8
 * @date 2023/3/27
 */
public class LeShanLwM2MServer implements LwM2MServer {

    private LeshanServer    server;

    private String  id;

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bindAddress;

    public LeShanLwM2MServer(String id) {
        this.id = id;
    }

    public void setLeShanServer(LeshanServer server) {
        this.server = server;
    }

    @Override
    public Flux<LwM2MExchangeMessage> handleRequest() {
        return null;
    }

    @Override
    public Flux<LwM2MAuthenticationRequest> handleAuth() {
        return null;
    }

    @Override
    public Mono<Void> send(LwM2MDownlinkMessage message) {
        return null;
    }

    @Override
    public void startUp() {

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return null;
    }

    @Override
    public void shutdown() {
        if (server != null) {
            server.destroy();
            server = null;
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
}
