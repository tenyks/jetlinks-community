package org.jetlinks.community.network.coap.server.lwm2m.impl;

import lombok.*;
import org.jetlinks.community.network.AbstractServerNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;

/**
 * LeShan的LwM2M服务端配置
 * @author v-lizy8
 * @date 2023/3/27
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeShanLwM2MServerProperties extends AbstractServerNetworkConfig {

    /**
     * 服务实例数量(线程数)
     */
    private int instance = Runtime.getRuntime().availableProcessors();

    @Override
    public NetworkTransport getTransport() {
        return NetworkTransport.UDP;
    }

    @Override
    public String getSchema() {
        return "coap";
    }
}
