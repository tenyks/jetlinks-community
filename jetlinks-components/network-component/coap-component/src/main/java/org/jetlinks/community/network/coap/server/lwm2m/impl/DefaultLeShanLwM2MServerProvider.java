package org.jetlinks.community.network.coap.server.lwm2m.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.security.SecurityStore;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
@Component
public class DefaultLeShanLwM2MServerProvider implements NetworkProvider<LeShanLwM2MServerProperties> {

    private static final String[] MODEL_PATHS = new String[]{
        "19.mxl"
    };

    /**
     * 响应等待时长，单位：毫秒
     */
    private static final long   RESPONSE_WAIT_TIME = 5000;

    private final CertificateManager certificateManager;

    private final DeviceRegistry deviceRegistry;

    private final SecurityStore securityStore;

    public DefaultLeShanLwM2MServerProvider(CertificateManager certificateManager,
                                            DeviceRegistry deviceRegistry,
                                            SecurityStore securityStore) {
        this.certificateManager = certificateManager;
        this.deviceRegistry = deviceRegistry;
        this.securityStore = securityStore;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.LWM2M_SERVER;
    }

    @Nonnull
    @Override
    public Mono<Network> createNetwork(@Nonnull LeShanLwM2MServerProperties properties) {
        log.warn("Start LwM2M server[{}]", properties.getId());

        LeShanLwM2MServer server = new LeShanLwM2MServer(properties.getId(), RESPONSE_WAIT_TIME);
        return initServer(server, properties);
    }

    @Override
    public Mono<Network> reload(@Nonnull Network network, @Nonnull LeShanLwM2MServerProperties properties) {
        log.warn("Reload LwM2M server[{}]", properties.getId());

        return initServer((LeShanLwM2MServer)network, properties);
    }

    @Override
    public boolean isReusable() {
        return true;
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("host", "本地地址", "", new StringType())
            .add("port", "本地端口", "", new IntType())
            .add("publicHost", "公网地址", "", new StringType())
            .add("publicPort", "公网端口", "", new IntType())
            .add("certId", "证书id", "", new StringType())
            .add("secure", "开启TSL", "", new BooleanType())
            .add("maxMessageSize", "最大消息长度", "", new StringType());
    }

    @Nonnull
    @Override
    public Mono<LeShanLwM2MServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            LeShanLwM2MServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new LeShanLwM2MServerProperties());
            config.setId(properties.getId());
            config.validate();
            return Mono.just(config);
        }).as(LocaleUtils::transform);
    }

    private Mono<Network>  initServer(LeShanLwM2MServer server, LeShanLwM2MServerProperties properties) {
        // 初始化lwm2m服务端
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(properties.getHost(), properties.getPort());
        builder.setCoapConfig(LeshanServerBuilder.createDefaultNetworkConfig());

        // 可支持的object模型
        List<ObjectModel> models = ObjectLoader.loadAllDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/", MODEL_PATHS));
        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        // lwm2m协议设备注册标识生成器
        builder.setRegistrationIdProvider(new Lwm2mRegistrationIdProvider());

        // 设置授权认证
        builder.setAuthorizer(server.buildAndBindAuthorizer(securityStore, deviceRegistry));
        LeshanServer lsServer = builder.build();

        // 设置监听器
        lsServer.getRegistrationService().addListener(server.buildAndBindRegistrationListener());
        lsServer.getPresenceService().addListener(server.buildAndBindPresenceListener());
        lsServer.getObservationService().addListener(server.buildAndBindObservationListener());

        server.setLeShanServer(lsServer);
        server.setBindAddress(new InetSocketAddress(properties.getHost(), properties.getPort()));

        return Mono.just(server);
    }
}
