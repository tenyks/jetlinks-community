package org.jetlinks.community.network.coap.server.lwm2m.impl;


import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Lwm2m协议订阅设备数据监听器
 */
public class Lwm2mObservationListener implements ObservationListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Flux<LwM2MMessage>      flux;

    private FluxSink<LwM2MMessage>  fluxSink;

    public Lwm2mObservationListener() {
        flux = Flux.create(sink -> this.fluxSink = sink);
    }

    @Override
    public void cancelled(Observation observation) {
        logger.warn("lwm2m2 observe cancelled ");
    }

    public Flux<LwM2MMessage> handleObservation() {
        return flux;
    }

    /**
     * 订阅后接收数据上报
     */
    @Override
    public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
        long startTime = System.currentTimeMillis();

        String  path = observation.getPath().toString();
        String  ep = registration.getEndpoint();
        LwM2mSingleResource content = (LwM2mSingleResource) response.getContent();
        String payload = new String(Hex.encodeHex((byte[]) content.getValue()));

//        fluxSink.next()
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
