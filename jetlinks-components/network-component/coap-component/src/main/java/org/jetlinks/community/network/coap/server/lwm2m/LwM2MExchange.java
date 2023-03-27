package org.jetlinks.community.network.coap.server.lwm2m;

import io.netty.buffer.Unpooled;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.leshan.core.ResponseCode;
import org.jetlinks.community.network.coap.device.LwM2MServerExchangeMessage;
import org.jetlinks.community.network.coap.server.coap.CoapExchange;
import org.jetlinks.community.network.coap.server.coap.CoapRequest;
import org.jetlinks.community.network.coap.server.coap.CoapResponse;
import org.jetlinks.core.message.codec.CoapResponseMessage;
import org.jetlinks.core.message.codec.DefaultCoapResponseMessage;
import org.jetlinks.core.message.codec.http.HttpExchangeMessage;
import org.jetlinks.core.trace.TraceHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

/**
 * LwM2M交换接口，支持获取请求和发送响应
 *
 * @author dumas.lee
 * @since 2.0
 */
public interface LwM2MExchange {
    /**
     * @return 请求ID
     */
    String requestId();

    /**
     * @return 时间戳
     */
    long timestamp();

    /**
     * @return 请求接口
     */
    LwM2MRequest request();

    /**
     * @return 响应接口
     */
    LwM2MResponse response();

    /**
     * @return 是否已经完成响应
     */
    boolean isClosed();


}
