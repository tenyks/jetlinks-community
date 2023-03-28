package org.jetlinks.community.network.coap.server.lwm2m.impl;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * @author v-lizy8
 * @date 2023/3/28
 */
public class Lwm2mRegistrationListener implements RegistrationListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void registered(Registration registration, Registration previousReg, Collection<Observation> collection) {
        // 转换为设备编码
        String deviceSn = Lwm2mRegistrationIdProvider.buildRegId(registration.getEndpoint());

        // 发送设备上线通知开始
        logger.debug("lwm2m online message {} messageArrived, send message success", deviceSn);

//        cmdSvc.sendLwM2mCmd(registration, Lwm2mConstant.Path.Path_19_0_0, Lwm2mConstant.CMD_TYPE.OBSERVE, null);
    }

    @Override
    public void updated(RegistrationUpdate update, Registration updatedRegistration, Registration previousRegistration) {
        logger.info("device [{}] updated", updatedRegistration.getEndpoint());
    }

    @Override
    public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                             Registration newReg) {
        // 设备下线通知
        logger.debug("lwm2m offline message {} messageArrived, send message success", registration.getEndpoint());
    }
}
