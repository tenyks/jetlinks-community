package org.jetlinks.community.network.coap.server.lwm2m.impl;


import io.netty.buffer.Unpooled;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.jetlinks.core.message.codec.lwm2m.LwM2MResource;
import org.jetlinks.core.message.codec.lwm2m.LwM2MUplinkMessage;
import org.jetlinks.core.message.codec.lwm2m.SimpleLwM2MUplinkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Lwm2m协议订阅设备数据监听器
 */
public class Lwm2mObservationListener implements ObservationListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Flux<LwM2MUplinkMessage>      flux;

    private FluxSink<LwM2MUplinkMessage>  fluxSink;

    public Lwm2mObservationListener() {
        flux = Flux.create(sink -> this.fluxSink = sink);
    }

    @Override
    public void cancelled(Observation observation) {
        logger.warn("lwm2m2 observe cancelled ");
    }

    public Flux<LwM2MUplinkMessage> handleObservation() {
        return flux;
    }

    /**
     * 订阅后接收数据上报
     */
    @Override
    public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
        String  path = observation.getPath().toString();
        String  ep = registration.getEndpoint();
        LwM2mSingleResource content = (LwM2mSingleResource) response.getContent();

        SimpleLwM2MUplinkMessage message = new SimpleLwM2MUplinkMessage();

        Response coapRsp = (Response) response.getCoapResponse();

        //TODO 根据observation.path确定是哪种资源
        message.setResource(LwM2MResource.BinaryAppDataContainerReport);
        message.setPayload(Unpooled.wrappedBuffer((byte[]) content.getValue()));
        message.setMessageId(coapRsp.getMID());
        message.setRegistrationId(registration.getId());

        fluxSink.next(message);
    }

    @Override
    public void onError(Observation observation, Registration registration, Exception error) {
        logger.error("lwm2m on new error {} {}", registration.getEndpoint(), observation.getPath(), error);
    }

    @Override
    public void newObservation(Observation observation, Registration registration) {
        logger.warn("lwm2m on new observation {} {}", registration.getEndpoint(), observation.getPath());
    }
}
