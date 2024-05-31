package org.jetlinks.community.network.coap.device;

import io.netty.buffer.Unpooled;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.server.registration.Registration;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MRegistrationEvent;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MServer;
import org.jetlinks.community.network.coap.server.lwm2m.impl.Lwm2mRegistrationIdProvider;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.community.utils.SystemUtils;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.*;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.lwm2m.*;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

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
    private static final AttributeKey<String> ENDPOINT = AttributeKey.stringKey("endpoint");

    final LwM2MServer                   server;

    /**
     * 定制协议
     */
    private Mono<ProtocolSupport>       custProtocol;

    private final DeviceRegistry        deviceRegistry;

    /**
     * 设备会话管理器
     */
    private final DeviceSessionManager  sessionManager;

    private final LongAdder counter = new LongAdder();

    private Disposable                  disposable;

    private final DeviceGatewayHelper   helper;


    public LwM2MServerDeviceGateway(String id,
                                    Mono<ProtocolSupport> protocol,
                                    DeviceRegistry deviceRegistry,
                                    DecodedClientMessageHandler clientMessageHandler,
                                    DeviceSessionManager sessionManager,
                                    LwM2MServer server) {
        super(id);

        this.custProtocol = protocol;
        this.server = server;
        this.deviceRegistry = deviceRegistry;
        this.sessionManager = sessionManager;
        this.helper = new DeviceGatewayHelper(this.deviceRegistry, sessionManager, clientMessageHandler);

    }

    public Mono<ProtocolSupport> getCustProtocol() {
        return custProtocol;
    }

    public void setCustProtocol(Mono<ProtocolSupport> custProtocol) {
        this.custProtocol = custProtocol;
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
            .flatMap(t -> this.handleAuthResponse(t.getT1(), t.getT2(), t.getT3()))
            .contextWrite(ReactiveLogger.start("network", server.getId()))
            .subscribe(
                ignore -> {},
                error -> log.error(error.getMessage(), error)
            );

        Disposable forRegEventDisposable = server
            .handleRegistrationEvent()
            .flatMap(this::handleRegistrationEvent)
            .subscribe(
                ignore -> {},
                error -> log.error(error.getMessage(), error)
            );

        Disposable forObservationDisposable = server
            .handleObservation()
            .publishOn(Schedulers.boundedElastic())
            .flatMap((this::decodeAndHandleMessage))
            .subscribe(
                ignore -> {},
                error -> log.error(error.getMessage(), error)
            );

        Disposable forReplyDisposable = server
            .handleReply()
            .publishOn(Schedulers.boundedElastic())
            .flatMap(exchangeMsg -> {
                if (exchangeMsg.isSuccess()) {
                    return this.decodeAndHandleMessage(exchangeMsg.getUplinkMessage());
                } else {
                    log.error("发生指令失败:{}", exchangeMsg);
                    return Mono.empty();
                }
            })
            .onErrorContinue((t, obj) -> {
                log.error("异常错误：msg={}", obj, t);
            })
            .subscribe(
                ignore -> {},
                error -> log.error(error.getMessage(), error)
            );

        this.disposable = Disposables.composite(forAuthDisposable, forRegEventDisposable,
                                    forObservationDisposable, forReplyDisposable);
    }

    /**
     * 解码消息并处理
     */
    private Mono<Void> decodeAndHandleMessage(LwM2MUplinkMessage message) {
        monitor.receivedMessage();

        String deviceId = Lwm2mRegistrationIdProvider.extractEndpointName(message.getRegistrationId());
        if (deviceId == null) {
            log.warn("无会话信息:regId={}, mid={}", message.getRegistrationId(), message.getMessageId());
            return Mono.empty();
        }
        if (log.isDebugEnabled()) {
            log.debug("[LwM2M]接收到消息：{}", message);
        }

        return sessionManager
            .getSession(deviceId)
            .flatMapMany(session -> {
                ((SimpleLwM2MUplinkMessage)message).setDeviceId(session.getDeviceId());

                return session
                        .getOperator()
                        .getProtocol()
                        .flatMap(protocol -> protocol.getMessageCodec(getTransport()))
                        /*解码*/
                        .flatMapMany(codec -> {
                            return codec.decode(FromDeviceMessageContext.of(session, message, deviceRegistry));
                        })
                        .cast(DeviceMessage.class)
                        .flatMap(msg -> {
                            //回填deviceId,有的场景协议包不能或者没有解析出deviceId,则直接使用连接对应的设备id进行填充.
                            if (!StringUtils.isNoneEmpty(msg.getDeviceId())) {
                                msg.thingId(DeviceThingType.device, session.getDeviceId());
                            }
                            return handleMessage((LwM2MDeviceSession) session, msg);
                        })
                        .as(FluxTracer
                            .create(DeviceTracer.SpanName.decode(session.getDeviceId()),
                                (span, msg) -> span.setAttribute(DeviceTracer.SpanKey.message, toJsonString(msg.toJson())))
                        )
                        //发生错误不中断流
                        .onErrorResume((err) -> {
                            log.error("handle mqtt message [{}] error:{}", session.getDeviceId(), message, err);
                            return Mono.empty();
                        })
                        .doOnComplete(() -> {
                            //无需返回消息
                        })
                    ;
            })
            .then();
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
                log.warn("无法从LwM2M[{}]消息中获取设备信息:{}", session.getDeviceId(), message);
            })
            .thenReturn(message);
    }

    /**
     * 处理设备注册相关事件，包括：设备上线、设备下线、会话更新等；
     */
    private Mono<Void> handleRegistrationEvent(LwM2MRegistrationEvent event) {
        log.info("[LwM2M]设备上下线事件：{}", event);

        if (event.isOfOnline()) {
            // 默认向设备发送Observe /19/0/0的指令
            SimpleLwM2MDownlinkMessage observeMsg = new SimpleLwM2MDownlinkMessage();
            observeMsg.setPath(LwM2MResource.BinaryAppDataContainerReport.getPath());
            observeMsg.setRegistrationId(event.getRegistrationId());
            observeMsg.setPayload(Unpooled.EMPTY_BUFFER);
            observeMsg.setRequestOperation(LwM2MOperation.Observe);

            return server.send(observeMsg).then();
        }
        if (event.isOfOffline()) {
            counter.decrement();

            //监控信息
            monitor.disconnected();
            monitor.totalConnection(counter.sum());

            String deviceId = event.getEndpoint();

            return sessionManager
                .getSession(deviceId, false)
                .cast(LwM2MDeviceSession.class)
                .flatMap(session -> {
                    if (session.getRegistrationId().equals(event.getRegistrationId())) {
                        return sessionManager.remove(deviceId, true).then();
                    } else {
                        return Mono.empty();
                    }
                });
        }
//
//        if (event.isOfUpdate() && event.getRegistrationId() != null) {
//            LwM2MDeviceSession oldSession = null;
//            if (event.getOldRegistrationId() != null) {
//                oldSession = regId2DevIdIdx.get(event.getOldRegistrationId());
//            }
//
//            if (!event.getRegistrationId().equals(event.getOldRegistrationId()) && oldSession != null) {
//                // 注册标识已经变更，更新会话对象
//                LwM2MDeviceSession newSession = new LwM2MDeviceSession(oldSession.getOperator(), event.getRegistrationId(), oldSession.getMessageSender());
//
//                regId2DevIdIdx.put(event.getRegistrationId(), newSession);
//
//                return Mono.empty();
//            }
//
//            if (!regId2DevIdIdx.containsKey(event.getRegistrationId())) {
//                // 确保新的注册标识对应的会话对象
//                return deviceRegistry.getDevice(event.getEndpoint())
//                    .flatMap(device -> {
//                        LwM2MDeviceSession newSession = new LwM2MDeviceSession(device, event.getRegistrationId(), server::send);
//
//                        regId2DevIdIdx.put(event.getRegistrationId(), newSession);
//                        return Mono.empty();
//                    })
//                    ;
//            }
//
//        }

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
                .publishOn(Schedulers.boundedElastic())
                //使用自定义协议来认证
                .map(support -> support.authenticate(request, deviceRegistry))
                //没有指定自定义协议,则使用endpoint对应的设备进行认证.
                .defaultIfEmpty(Mono.defer(() -> deviceRegistry.getDevice(authReq.getEndpoint()).flatMap(device -> device.authenticate(request))))
                .flatMap(Function.identity())
                //如果认证结果返回空,说明协议没有设置认证,或者认证返回不对, 默认返回BAD_USER_NAME_OR_PASSWORD,防止由于协议编写不当导致mqtt任意访问的安全问题.
                .switchIfEmpty(Mono.fromRunnable(() -> request.reject("BAD_USER_NAME_OR_PASSWORD"))))
            .flatMap(resp -> {
                log.info("[LwM2M]设备认证结果：req={}, resp={}", authReq, resp);

                //认证响应可以自定义设备ID,如果没有则使用请求URI中的ep参数
                String deviceId = StringUtils.isEmpty(resp.getDeviceId()) ? authReq.getEndpoint() : resp.getDeviceId();

                //认证返回了新的设备ID,则使用新的设备
                return deviceRegistry
                    .getDevice(deviceId)
                    .map(operator -> Tuples.of(operator, resp, authReq))
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
                    span.setAttribute(ENDPOINT, authReq.getEndpoint());
                })
            )
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
            if (resp.isSuccess()) {
                //认证通过
                counter.increment();

                return sessionManager
                    .compute(deviceId, old -> {
                        LwM2MDeviceSession newSession = new LwM2MDeviceSession(device, (Registration) authReq.getRegistration(), server::send);
                        return old
                            .cast(LwM2MDeviceSession.class)
                            .<DeviceSession>map(session -> {
//                                regId2DevIdIdx.put(session.getRegistrationId(), newSession);
                                return newSession;
                            })
                            .defaultIfEmpty(newSession);
                    })
                    .cast(LwM2MDeviceSession.class)
                    .doOnNext(o -> {
                        //监控信息
                        monitor.connected();
                        monitor.totalConnection(counter.sum());

                        authReq.accept();
                        log.info("[LwM2M]认证通过：req={}, resp={}", authReq, resp);
                    })
                    //会话empty说明注册会话失败?
                    .switchIfEmpty(Mono.fromRunnable(() -> authReq.reject("CONNECTION_REFUSED_IDENTIFIER_REJECTED")))
                    ;
            } else {
                //认证失败返回 0x04 BAD_USER_NAME_OR_PASSWORD
                authReq.reject("CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD");
                monitor.rejected();
                log.warn("LwM2M客户端认证[{}]失败:{}", deviceId, resp.getMessage());
                return Mono.empty();
            }
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
