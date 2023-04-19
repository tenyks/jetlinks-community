package org.jetlinks.community.network.coap.device;

import org.eclipse.leshan.server.registration.Registration;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.lwm2m.LwM2MDownlinkMessage;
import org.jetlinks.core.message.codec.lwm2m.SimpleLwM2MDownlinkMessage;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.function.Function;

/**
 * LwM2M设备的会话
 *
 * @author v-lizy8
 * @date 2023/3/27
 */
public class LwM2MDeviceSession implements DeviceSession {

    private final DeviceOperator operator;

    private final Registration registration;

    /**
     * 设备注册时分配的标识，与会话标识等同
     */
    private final String        registrationId;

    private final Function<LwM2MDownlinkMessage, Mono<Boolean>> messageSender;

    private long lastPingTime = System.currentTimeMillis();

    //默认永不超时
    private long keepAliveTimeOutMs = -1;

    public LwM2MDeviceSession(DeviceOperator deviceOperator,
                              Registration registration,
                              Function<LwM2MDownlinkMessage, Mono<Boolean>> messageSender) {
        this.operator = deviceOperator;
        this.registration = registration;
        this.messageSender = messageSender;
        this.registrationId = registration.getId();
    }

    public LwM2MDeviceSession(DeviceOperator deviceOperator,
                              String registrationId,
                              Function<LwM2MDownlinkMessage, Mono<Boolean>> messageSender) {
        this.operator = deviceOperator;
        this.messageSender = messageSender;
        this.registrationId = registrationId;
        this.registration = null;
    }

    @Override
    public String getId() {
        return operator.getDeviceId();
    }

    @Override
    public String getDeviceId() {
        return operator.getDeviceId();
    }

    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return operator;
    }

    @Override
    public long lastPingTime() {
        return lastPingTime;
    }

    @Override
    public long connectTime() {
        return lastPingTime;
    }

    public Function<LwM2MDownlinkMessage, Mono<Boolean>> getMessageSender() {
        return messageSender;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        //TODO 补充会话相关字段
        if (!(encodedMessage instanceof LwM2MDownlinkMessage)) {
            return Mono.just(false);
        }

        LwM2MDownlinkMessage dlMsg = (LwM2MDownlinkMessage) encodedMessage;
        if (dlMsg.getRegistrationId() == null) {
            if (dlMsg instanceof SimpleLwM2MDownlinkMessage) {
                ((SimpleLwM2MDownlinkMessage)dlMsg).setRegistrationId(registrationId);
            }
        }

        return messageSender.apply(dlMsg);
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.LwM2M;
    }

    @Override
    public void close() {

    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeOutMs = timeout.toMillis();
    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAlive() {
        return keepAliveTimeOutMs <= 0
            || System.currentTimeMillis() - lastPingTime < keepAliveTimeOutMs;
    }

    @Nullable
    @Deprecated
    public Registration getRegistration() {
        return registration;
    }

    @Nonnull
    public String getRegistrationId() {
        return registrationId;
    }

    @Override
    public void onClose(Runnable call) {

    }

}
