package org.jetlinks.community.network.coap.server.lwm2m;

import org.eclipse.leshan.server.registration.Registration;

/**
 * LwM2M设备睡眠/唤醒事件
 */
public interface LwM2MPresenceEvent {

    /**
     * @return  设备注册对象
     */
    Registration    getRegistration();

    /**
     * @return  如果是唤醒事件返回true，否则返回false；
     */
    boolean     isOfAwake();

}
