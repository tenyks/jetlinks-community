package org.jetlinks.community.network.coap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;

import java.nio.charset.StandardCharsets;

/**
 * @author dumas.lee
 * @since 2.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CoapMessage implements EncodedMessage {

    private ByteBuf payload;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (ByteBufUtil.isText(payload, StandardCharsets.UTF_8)) {
            builder.append(payloadAsString());
        } else {
            ByteBufUtil.appendPrettyHexDump(builder, payload);
        }

        return builder.toString();
    }
}
