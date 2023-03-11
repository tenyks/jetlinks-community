package org.jetlinks.community.network.coap.server;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.http.Header;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

/**
 * COAP响应信息
 *
 * @author dumas.lee
 * @since 2.0
 */
public interface CoapResponse {
    /**
     * 设置响应状态码
     *
     * @param status 状态吗
     * @return this
     */
    CoapResponse status(int status);

    /**
     * 设置响应类型
     *
     * @param mediaType 媒体类型
     * @return this
     */
    CoapResponse contentType(MediaType mediaType);

    /**
     * 设置响应头
     *
     * @param header 响应头
     * @return this
     */
    CoapResponse header(Header header);

    /**
     * 设置响应头
     *
     * @param header key
     * @param value  value
     * @return this
     */
    CoapResponse header(String header, String value);

    /**
     * 写出数据
     *
     * @param buffer ByteBuf
     * @return void
     */
    Mono<Void> write(ByteBuf buffer);

    /**
     * 完成响应
     *
     * @return void
     */
    Mono<Void> end();

    /**
     * 响应数据然后结束
     *
     * @param buffer ByteBuf
     * @return void
     */
    default Mono<Void> writeAndEnd(ByteBuf buffer) {
        return write(buffer).then(end());
    }
}
