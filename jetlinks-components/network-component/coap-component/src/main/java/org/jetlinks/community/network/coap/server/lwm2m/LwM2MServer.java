package org.jetlinks.community.network.coap.server.lwm2m;

import org.jetlinks.community.network.ServerNetwork;
import reactor.core.publisher.Flux;

/**
 * LwM2M服务端
 *
 * @author dumas.lee
 * @version 2.0
 * @since 2.0
 */
public interface LwM2MServer extends ServerNetwork {

    /**
     * 监听所有请求
     *
     * @return LwM2MExchange
     */
    Flux<LwM2MExchange> handleRequest();

    void startUp();

    /**
     * 停止服务
     */
    void shutdown();
}
