package org.jetlinks.community.network.coap.server.lwm2m.impl;

import lombok.*;
import org.jetlinks.community.network.AbstractServerNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;

/**
 * @author v-lizy8
 * @date 2023/3/27
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
//@NoArgsConstructor
public class LwM2MServerConfig extends AbstractServerNetworkConfig {
    @Override
    public NetworkTransport getTransport() {
        return NetworkTransport.UDP;
    }

    @Override
    public String getSchema() {
        return "coap";
    }
}
