package org.jetlinks.community.network.coap.server.lwm2m.impl;

import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lwm2m协议设备休眠和唤醒状态，暂时不需要推送该数据
 */
public class Lwm2mPresenceListener implements PresenceListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void onSleeping(Registration registration) {
        if (logger.isDebugEnabled()) {
            logger.debug("lwm2m device {} is on sleeping", registration.getEndpoint());
        }
    }

    @Override
    public void onAwake(Registration registration) {
        if (logger.isDebugEnabled()) {
            logger.debug("lwm2m device {} is on awake", registration.getEndpoint());
        }
    }
}
