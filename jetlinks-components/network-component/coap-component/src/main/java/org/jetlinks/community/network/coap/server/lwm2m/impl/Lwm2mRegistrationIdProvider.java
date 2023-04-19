package org.jetlinks.community.network.coap.server.lwm2m.impl;

import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.server.registration.RegistrationIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LwM2M协议设备注册标识生成器
 *
 * @author v-lizy8
 * @date 2023/3/27
 */
public class Lwm2mRegistrationIdProvider implements RegistrationIdProvider {

    private static final Logger log = LoggerFactory.getLogger(Lwm2mRegistrationIdProvider.class);

    private static SecureRandom Rand = new SecureRandom(String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));

    @Override
    public String getRegistrationId(RegisterRequest registerRequest) {
        String regId = buildRegId(registerRequest.getEndpointName());

        log.debug("provide regId={}", regId);

        return regId;
    }

    public static String extractEndpointName(String regId) {
        if (!regId.startsWith("SSID:") || regId.length() < 13) return null;

        return regId.substring(5, regId.length() - 7);
    }

    public static String buildRegId(String endpointName) {
        return String.format("SSID:%s:%07d", endpointName, Rand.nextInt() % 10000000);
    }

}
