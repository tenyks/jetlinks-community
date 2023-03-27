package org.jetlinks.community.network.coap.server.lwm2m.impl;

import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.server.registration.RegistrationIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LwM2M协议设备注册标识生成器
 *
 * @author v-lizy8
 * @date 2023/3/27
 */
public class Lwm2mRegistrationIdProvider implements RegistrationIdProvider {

    private static final Logger log = LoggerFactory.getLogger(Lwm2mRegistrationIdProvider.class);

    @Override
    public String getRegistrationId(RegisterRequest registerRequest) {
        String regId = buildRegId(registerRequest.getEndpointName());

        log.debug("provide regid={}", regId);

        return regId;
    }

    public static String buildRegId(String endpointName) {
        return "IAPRID:" + endpointName;
    }
}
