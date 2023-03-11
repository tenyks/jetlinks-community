package org.jetlinks.community.network.coap.server;

import org.jetlinks.community.network.ServerNetwork;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Flux;

import java.util.Locale;


/**
 * Coap服务
 *
 * @author tenyks
 * @version 1.0
 **/
public interface CoapServer extends ServerNetwork {

    /**
     * 监听所有请求
     *
     * @return CoapExchange
     */
    Flux<CoapExchange> handleRequest();

    /**
     * 根据请求方法和url监听请求.
     * <p>
     * URL支持通配符:
     * <pre>
     *   /device/* 匹配/device/下1级的请求,如: /device/1
     *
     *   /device/** 匹配/device/下N级的请求,如: /device/1/2/3
     *
     * </pre>
     *
     * @param method        请求方法: {@link org.springframework.http.HttpMethod}
     * @param urlPattern    url
     * @return HttpExchange
     */
    Flux<CoapExchange> handleRequest(String method, String... urlPattern);


    default Flux<CoapExchange> handleRequest(HttpMethod method, String... urlPattern) {
        return handleRequest(method.name().toLowerCase(Locale.ROOT), urlPattern);
    }

    /**
     * 停止服务
     */
    void shutdown();
}
