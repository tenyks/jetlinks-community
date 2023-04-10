package org.jetlinks.community.network.coap.device;

import io.netty.buffer.ByteBuf;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MExchange;
import org.jetlinks.core.message.codec.http.MultiPart;

public class LwM2MServerExchangeMessage extends CoapServerExchangeMessage {
    public LwM2MServerExchangeMessage(LwM2MExchange exchange, ByteBuf payload, MultiPart multiPart) {
//        super(exchange, payload, multiPart);
    }
}
