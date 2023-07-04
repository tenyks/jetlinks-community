package org.jetlinks.community.network.coap.server.coap;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.parser.PayloadParser;
import org.jetlinks.community.network.parser.PayloadParserBuilder;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.community.network.security.VertxKeyCertTrustOptions;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author bestfeng
 */
@Slf4j
@Component
public class DefaultCoapServerProvider implements NetworkProvider<CoapServerProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    private final PayloadParserBuilder payloadParserBuilder;

    public DefaultCoapServerProvider(CertificateManager certificateManager, Vertx vertx, PayloadParserBuilder payloadParserBuilder) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
        this.payloadParserBuilder = payloadParserBuilder;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Nonnull
    @Override
    public Mono<Network> createNetwork(@Nonnull CoapServerProperties properties) {

        CaliforniumCoapServer tcpServer = new CaliforniumCoapServer(properties.getId());
        return initTcpServer(tcpServer, properties);
    }

    private Mono<Network> initTcpServer(CaliforniumCoapServer coapServer, CoapServerProperties properties) {
        return convert(properties)
            .map(options -> {
                int instance = Math.max(2, properties.getInstance());
                List<NetServer> instances = new ArrayList<>(instance);
                for (int i = 0; i < instance; i++) {
                    instances.add(vertx.createNetServer(options));
                }
                Supplier<PayloadParser> parserSupplier= payloadParserBuilder.build(properties.getParserType(), properties);
                parserSupplier.get();

                coapServer.setParserSupplier(parserSupplier);
                coapServer.setServer(instances);
                coapServer.setKeepAliveTimeout(properties.getLong("keepAliveTimeout", Duration
                    .ofMinutes(10)
                    .toMillis()));
                coapServer.setBind(new InetSocketAddress(properties.getHost(), properties.getPort()));
                for (NetServer netServer : instances) {
                    vertx.nettyEventLoopGroup()
                        .execute(()->{
                            netServer.listen(properties.createSocketAddress(), result -> {
                                if (result.succeeded()) {
                                    log.info("tcp server startup on {}", result.result().actualPort());
                                } else {
                                    coapServer.setLastError(result.cause().getMessage());
                                    log.error("startup tcp server error", result.cause());
                                }
                            });
                        });
                }
                return coapServer;
            });
    }

    @Override
    public Mono<Network> reload(@Nonnull Network network, @Nonnull CoapServerProperties properties) {
        CaliforniumCoapServer tcpServer = ((CaliforniumCoapServer) network);
        tcpServer.shutdown();
        return initTcpServer(tcpServer, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("host", "本地地址", "", new StringType())
            .add("port", "本地端口", "", new IntType())
            .add("publicHost", "公网地址", "", new StringType())
            .add("publicPort", "公网端口", "", new IntType())
            .add("certId", "CA证书", "", new StringType().expand("selector", "cert"))
            .add("secure", "开启TSL", "", new BooleanType())
            .add("parserType", "解析器类型", "", new ObjectType())
            .add("parserConfiguration", "配置解析器", "", new ObjectType());
    }

    @Nonnull
    @Override
    public Mono<CoapServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.fromSupplier(() -> {
            CoapServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new CoapServerProperties());
            config.setId(properties.getId());
            config.validate();
            return config;
        })
            .as(LocaleUtils::transform);
    }

    private Mono<NetServerOptions> convert(CoapServerProperties properties) {
        NetServerOptions options = new NetServerOptions();
        options.setPort(properties.getPort());
        options.setTcpKeepAlive(true);

        if (properties.isSecure()) {
            options.setSsl(true);
            return certificateManager
                .getCertificate(properties.getCertId())
                .map(VertxKeyCertTrustOptions::new)
                .doOnNext(options::setKeyCertOptions)
                .doOnNext(options::setTrustOptions)
                .thenReturn(options);
        }
        return Mono.just(options);
    }
}
