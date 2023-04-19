package org.jetlinks.community.network.coap.device;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.registration.Registration;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MRegistrationEvent;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MServer;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.community.utils.SystemUtils;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.*;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.lwm2m.LwM2MMessage;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/**
 * LwM2M协议网关
 *
 * @author tenyks
 * @version 2.0
 */
@Slf4j
public class LwM2MServerDeviceGateway extends AbstractDeviceGateway {
    private static AttributeKey<String> clientId = AttributeKey.stringKey("clientId");

    final LwM2MServer server;

    private final Mono<ProtocolSupport> custProtocol;

    private final DeviceRegistry registry;

    /**
     * 设备会话管理器
     */
    private final DeviceSessionManager sessionManager;

    private final LongAdder counter = new LongAdder();

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    /**
     * 缓存的Session
     * //FIXME 补充自动清退已离线的会话
     */
    private final ConcurrentMap<String, LwM2MDeviceSession>     sessionPool;

    public LwM2MServerDeviceGateway(String id,
                                    Mono<ProtocolSupport> protocol,
                                    DeviceRegistry deviceRegistry,
                                    DecodedClientMessageHandler clientMessageHandler,
                                    DeviceSessionManager sessionManager,
                                    LwM2MServer server) {
        super(id);

        this.custProtocol = protocol;
        this.server = server;
        this.registry = deviceRegistry;
        this.sessionManager = sessionManager;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);

        this.sessionPool = new ConcurrentReferenceHashMap<>();
    }

    public Mono<ProtocolSupport> getCustProtocol() {
        return custProtocol;
    }

    public Transport getTransport() {
        return DefaultTransport.LwM2M;
    }

    public NetworkType getNetworkType() {
        return DefaultNetworkType.LWM2M_SERVER;
    }

    private void doStart() {
        if (disposable != null) {
            disposable.dispose();
        }

        Disposable forAuthDisposable = server
            .handleAuthentication()
            .filter(authReq -> {
                //暂停或者已停止时.
                if (!isStarted()) {
                    //直接响应SERVER_UNAVAILABLE
                    authReq.reject("SERVER_UNAVAILABLE");
                    monitor.rejected();
                }

                return true;
            })
            .publishOn(Schedulers.parallel())
            .flatMap(this::handleAuthRequest, Integer.MAX_VALUE)
            .contextWrite(ReactiveLogger.start("network", server.getId()))
            .subscribe(
                ignore -> {},
                error -> log.error(error.getMessage(), error)
            );

        Disposable forRegEventDisposable = server
            .handleRegistrationEvent()
            .flatMap(event -> {

            })
            .subscribe(
                ignore -> {},
                error -> log.error(error.getMessage(), error)
            );

        Disposable forObservationDisposable = server
            .handleObservation()
            .publishOn(Schedulers.boundedElastic())
            .flatMap(observation -> {

            });

    }

    //解码消息并处理
    private Mono<Void> decodeAndHandleMessage(LwM2MDeviceSession session,
                                              LwM2MMessage message) {
        monitor.receivedMessage();

        DeviceOperator operator = session.getOperator();
        return operator
            .getProtocol()
            .flatMap(protocol -> protocol.getMessageCodec(getTransport()))
            //解码
            .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(session, message, registry)))
            .cast(DeviceMessage.class)
            .flatMap(msg -> {
                //回填deviceId,有的场景协议包不能或者没有解析出deviceId,则直接使用连接对应的设备id进行填充.
                if (!StringUtils.hasText(msg.getDeviceId())) {
                    msg.thingId(DeviceThingType.device, operator.getDeviceId());
                }
                return this.handleMessage(operator, session, msg);
            })
            .doOnComplete(() -> {
                //ACK
            })
            .as(FluxTracer
                .create(DeviceTracer.SpanName.decode(operator.getDeviceId()),
                    (span, msg) -> span.setAttribute(DeviceTracer.SpanKey.message, toJsonString(msg.toJson()))))
            //发生错误不中断流
            .onErrorResume((err) -> {
                log.error("handle mqtt message [{}] error:{}", operator.getDeviceId(), message, err);
                return Mono.empty();
            })
            .then()
            ;
    }

    /**
     * 处理解码后的消息
     */
    private Mono<DeviceMessage> handleMessage(LwM2MDeviceSession session,
                                              DeviceMessage message) {
        //统一处理解码后的设备消息
        return helper.handleDeviceMessage(message,
            device -> session,
            s -> {

            },
            () -> {
                log.warn("无法从MQTT[{}]消息中获取设备信息:{}", session.getDeviceId(), message);
            })
            .thenReturn(message);
    }

    /**
     * 处理设备注册相关事件，包括：设备上线、设备下线、会话更新等；
     */
    private Mono<Void> handleRegistrationEvent(LwM2MRegistrationEvent event) {
        if (event.isOfOffline()) {
            sessionPool.remove(event.getRegistrationId());
            return Mono.empty();
        }

        if (event.isOfUpdate() && event.getRegistrationId() != null) {
            LwM2MDeviceSession oldSession = null;
            if (event.getOldRegistrationId() != null) {
                oldSession = sessionPool.get(event.getOldRegistrationId());
            }

            if (!event.getRegistrationId().equals(event.getOldRegistrationId()) && oldSession != null) {
                // 注册标识已经变更，更新会话对象
                LwM2MDeviceSession newSession = new LwM2MDeviceSession(oldSession.getOperator(), event.getRegistrationId(), oldSession.getMessageSender());

                sessionPool.put(event.getRegistrationId(), newSession);

                return Mono.empty();
            }

            if (!sessionPool.containsKey(event.getRegistrationId())) {
                // 确保新的注册标识对应的会话对象
                return registry.getDevice(event.getEndpoint())
                    .flatMap(device -> {
                        LwM2MDeviceSession newSession = new LwM2MDeviceSession(device, event.getRegistrationId(), server::send);

                        sessionPool.put(event.getRegistrationId(), newSession);
                        return Mono.empty();
                    })
                    .switchIfEmpty(null);
            }

        }

        return Mono.empty();
    }

    /**
     * 处理设备认证请求
     */
    private Mono<Tuple3<DeviceOperator, AuthenticationResponse, LwM2MAuthenticationRequest>>
    handleAuthRequest(LwM2MAuthenticationRequest authReq) {
        //内存不够了
        if (SystemUtils.memoryIsOutOfWatermark()) {
            //直接拒绝
            authReq.reject("内存不够，直接拒绝");
            return Mono.empty();
        }

        return Mono
            .justOrEmpty(authReq)
            .flatMap(request -> custProtocol
                //使用自定义协议来认证
                .map(support -> support.authenticate(request, registry))
                //没有指定自定义协议,则使用endpoint对应的设备进行认证.
                .defaultIfEmpty(Mono.defer(() -> registry.getDevice(authReq.getEndpoint()).flatMap(device -> device.authenticate(request))))
                .flatMap(Function.identity())
                //如果认证结果返回空,说明协议没有设置认证,或者认证返回不对, 默认返回BAD_USER_NAME_OR_PASSWORD,防止由于协议编写不当导致mqtt任意访问的安全问题.
                .switchIfEmpty(Mono.fromRunnable(() -> request.reject("默认返回BAD_USER_NAME_OR_PASSWORD"))))
            .flatMap(resp -> {
                //认证响应可以自定义设备ID,如果没有则使用请求URI中的ep参数
                String deviceId = StringUtils.isEmpty(resp.getDeviceId()) ? authReq.getEndpoint() : resp.getDeviceId();

                //认证返回了新的设备ID,则使用新的设备
                return registry
                    .getDevice(deviceId)
                    .map(operator -> {
                        LwM2MDeviceSession session = new LwM2MDeviceSession(operator, (Registration) authReq.getRegistration(), server::send);

                        return Tuples.of(operator, resp, authReq);
                    })
                    //设备不存在,应答IDENTIFIER_REJECTED
                    .switchIfEmpty(Mono.fromRunnable(() -> authReq.reject("设备不存在, 应答IDENTIFIER_REJECTED")))
                    ;
            })
            .as(MonoTracer.create(DeviceTracer.SpanName.auth(authReq.getIdentity()),
                (span, tp3) -> {
                    AuthenticationResponse response = tp3.getT2();
                    if (!response.isSuccess()) {
                        span.setStatus(StatusCode.ERROR, response.getMessage());
                    }
                },
                (span, hasValue) -> {
                    //empty
                    if (!hasValue) {
                        span.setStatus(StatusCode.ERROR, "device not exists");
                    }
                    span.setAttribute(DeviceTracer.SpanKey.address, authReq.getClientAddress());
                    span.setAttribute(clientId, authReq.getIdentity());
                }))
            //设备认证异常,拒绝连接
            .onErrorResume((err) -> Mono.fromRunnable(() -> {
                log.error("LwM2M认证[{}]失败", authReq.getIdentity(), err);
                //监控信息
                monitor.rejected();
                authReq.reject("认证异常失败");
            }))
            ;
    }

    /**
     * 处理设备认证结果：创建设备会话，响应认证请求
     */
    private Mono<LwM2MDeviceSession>
    handleAuthResponse(DeviceOperator device, AuthenticationResponse resp, LwM2MAuthenticationRequest authReq) {
        return Mono.defer(() -> {
            String deviceId = device.getDeviceId();
            //认证通过
            if (resp.isSuccess()) {
                counter.increment();

                return sessionManager
                    .compute(deviceId, old -> {
                        LwM2MDeviceSession newSession = new LwM2MDeviceSession(device, (Registration) authReq.getRegistration(), server::send);
                        return old
                            .cast(LwM2MDeviceSession.class)
                            .<DeviceSession>map(session -> {
                                sessionPool.put(session.getRegistrationId(), newSession);
                                return newSession;
                            })
                            .defaultIfEmpty(newSession);
                    })
                    .cast(LwM2MDeviceSession.class)
                    .doOnNext(o -> {
                        //监控信息
                        monitor.connected();
                        monitor.totalConnection(counter.sum());
                    })
                    //会话empty说明注册会话失败?
                    .switchIfEmpty(Mono.fromRunnable(() -> authReq.reject("CONNECTION_REFUSED_IDENTIFIER_REJECTED")));
            } else {
                //认证失败返回 0x04 BAD_USER_NAME_OR_PASSWORD
                authReq.reject("CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD");
                monitor.rejected();
                log.warn("MQTT客户端认证[{}]失败:{}", deviceId, resp.getMessage());
            }

            return Mono.empty();
        })
        .onErrorResume(error -> Mono.fromRunnable(() -> {
            log.error(error.getMessage(), error);
            monitor.rejected();
            //发生错误时应答 SERVER_UNAVAILABLE
            authReq.reject("CONNECTION_REFUSED_SERVER_UNAVAILABLE");
        }));
    }

    @Override
    protected Mono<Void> doStartup() {
        return Mono.fromRunnable(this::doStart);
    }

    @SneakyThrows
    private String toJsonString(Object data){
        return ObjectMappers.JSON_MAPPER.writeValueAsString(data);
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
