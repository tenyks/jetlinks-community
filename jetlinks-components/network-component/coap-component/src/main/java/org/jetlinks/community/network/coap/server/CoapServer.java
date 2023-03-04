package org.jetlinks.community.network.coap.server;

import org.jetlinks.community.network.ServerNetwork;
import org.jetlinks.community.network.coap.client.CoapClient;
import reactor.core.publisher.Flux;


/**
 * Coap服务
 *
 * @author tenyks
 * @version 1.0
 **/
public interface CoapServer extends ServerNetwork {

    /**
     * 订阅客户端连接
     *
     * @return 客户端流
     * @see CoapClient
     */
    Flux<CoapClient> handleConnection();

    /**
     * 关闭服务端
     */
    void shutdown();
}
