package org.jetlinks.community.network.coap.server.lwm2m;

/**
 * LwM2M上下线事件，分别是：设备上线、注册更新、设备下线
 */
public interface LwM2MRegistrationEvent {

    /**
     * @return  对应LwM2M协议的ep参数
     */
    String  getEndpoint();

    /**
     * @return  如果是更新在线的事件，返回之前的注册标识
     */
    String  getOldRegistrationId();

    /**
     * @return  注册标识
     */
    String  getRegistrationId();

    /**
     * @return  如果是设备上线，返回true；
     */
    boolean isOfOnline();

    /**
     * @return  如果是设备下线，返回true
     */
    boolean isOfOffline();

    /**
     * @return  如果是更新在线，返回true
     */
    boolean isOfUpdate();

}
