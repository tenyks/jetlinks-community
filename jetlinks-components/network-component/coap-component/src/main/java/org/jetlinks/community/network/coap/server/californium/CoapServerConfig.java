package org.jetlinks.community.network.coap.server.californium;

import lombok.*;
import org.jetlinks.community.network.AbstractServerNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;

/**
 * Coap服务配置
 *
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@Builder
@AllArgsConstructor

public class CoapServerConfig extends AbstractServerNetworkConfig {

    @Override
    public NetworkTransport getTransport() {
        return null;
    }

    @Override
    public String getSchema() {
        return null;
    }
}
