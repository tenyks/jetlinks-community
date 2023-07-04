package org.jetlinks.community.network.coap.server.coap;

import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.parser.PayloadParser;
import org.jetlinks.core.utils.Reactors;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class CaliforniumCoapServer implements CoapServer {

    Collection<NetServer> coapServers;

    private Supplier<PayloadParser> parserSupplier;

    @Setter
    private long keepAliveTimeout = Duration.ofMinutes(10).toMillis();

    @Getter
    private final String id;

    private final Sinks.Many<CoapClient> sink = Reactors.createMany(Integer.MAX_VALUE,false);

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bind;

    public CaliforniumCoapServer(String id) {
        this.id = id;
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close tcp server error", e);
        }
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return bind;
    }

    public void setParserSupplier(Supplier<PayloadParser> parserSupplier) {
        this.parserSupplier = parserSupplier;
    }

    public void setServer(Collection<NetServer> servers) {
        if (this.coapServers != null && !this.coapServers.isEmpty()) {
            shutdown();
        }
        this.coapServers = servers;

        for (NetServer tcpServer : this.coapServers) {
            tcpServer.connectHandler(this::acceptTcpConnection);
        }

    }

    protected void acceptTcpConnection(NetSocket socket) {
        if (sink.currentSubscriberCount() == 0) {
            log.warn("not handler for tcp client[{}]", socket.remoteAddress());
            socket.close();
            return;
        }
//        CaliforniumCoapClient client = new CaliforniumCoapClient(id + "_" + socket.remoteAddress());
//        client.setKeepAliveTimeoutMs(keepAliveTimeout);
//        try {
//            socket.exceptionHandler(err -> {
//                log.error("tcp server client [{}] error", socket.remoteAddress(), err);
//            });
//            client.setRecordParser(parserSupplier.get());
//            client.setSocket(socket);
//            sink.emitNext(client, Reactors.emitFailureHandler());
//            log.debug("accept tcp client [{}] connection", socket.remoteAddress());
//        } catch (Exception e) {
//            log.error("create tcp server client error", e);
//            client.shutdown();
//        }
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Override
    public Flux<CoapExchange> handleRequest() {
        return null;
    }

    @Override
    public Flux<CoapExchange> handleRequest(String method, String... urlPattern) {
        return null;
    }

    @Override
    public Flux<CoapExchange> handleRequest(HttpMethod method, String... urlPattern) {
        return CoapServer.super.handleRequest(method, urlPattern);
    }

    @Override
    public void shutdown() {
        if (null != coapServers) {
            log.debug("close tcp server :[{}]", id);
            for (NetServer server : coapServers) {
                execute(server::close);
            }
            coapServers = null;
        }
    }

    @Override
    public boolean isAlive() {
        return coapServers != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
