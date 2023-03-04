package org.jetlinks.community.network.coap.client;

import io.vertx.core.net.NetClientOptions;
import lombok.*;
import org.jetlinks.community.network.parser.PayloadParserType;
import org.jetlinks.community.ValueObject;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *  @author tenyks
 *  @version 1.0
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CoapClientProperties implements ValueObject {

    private String id;

    private int port;

    private String host;

    private String certId;

    private boolean ssl;

    private PayloadParserType parserType = PayloadParserType.DIRECT;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    private NetClientOptions options;

    private boolean enabled;

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }
}
