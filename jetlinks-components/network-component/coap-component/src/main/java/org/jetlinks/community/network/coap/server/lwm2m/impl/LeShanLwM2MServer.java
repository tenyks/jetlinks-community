package org.jetlinks.community.network.coap.server.lwm2m.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MExchange;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MServer;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

/**
 * @author v-lizy8
 * @date 2023/3/27
 */
public class LeShanLwM2MServer implements LwM2MServer {

    private LeshanServer    server;

    private LwM2MServerConfig   config;

    private String  id;

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bindAddress;

    public LeShanLwM2MServer(LwM2MServerConfig config) {
        this.config = config;
        this.id = config.getId();
    }

    public void setHttpServers(LeshanServer server) {

    }

    @Override
    public Flux<LwM2MExchange> handleRequest() {
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
