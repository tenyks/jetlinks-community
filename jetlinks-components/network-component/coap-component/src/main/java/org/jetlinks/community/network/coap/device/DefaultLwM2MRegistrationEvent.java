package org.jetlinks.community.network.coap.device;

import org.jetlinks.community.network.coap.server.lwm2m.LwM2MRegistrationEvent;

public class DefaultLwM2MRegistrationEvent implements LwM2MRegistrationEvent {

    private String  endpoint;

    private String  registrationId;

    private String  oldRegistrationId;

    private int     type;

    private DefaultLwM2MRegistrationEvent(int type, String endpoint, String registrationId, String oldRegistrationId) {
        this.type = type;
        this.endpoint = endpoint;
        this.registrationId = registrationId;
    }

    public static DefaultLwM2MRegistrationEvent ofOnline(String endpoint, String registrationId) {
        return new DefaultLwM2MRegistrationEvent(1, endpoint, registrationId, null);
    }

    public static DefaultLwM2MRegistrationEvent ofUpdate(String endpoint, String registrationId, String oldRegistrationId) {
        return new DefaultLwM2MRegistrationEvent(9, endpoint, registrationId, oldRegistrationId);
    }

    public static DefaultLwM2MRegistrationEvent ofOffline(String endpoint, String registrationId) {
        return new DefaultLwM2MRegistrationEvent(0, endpoint, registrationId, null);
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public String getOldRegistrationId() {
        return oldRegistrationId;
    }

    @Override
    public String getRegistrationId() {
        return registrationId;
    }

    @Override
    public boolean isOfOnline() {
        return (type == 1);
    }

    @Override
    public boolean isOfOffline() {
        return (type == 0);
    }

    @Override
    public boolean isOfUpdate() {
        return (type == 9);
    }

    @Override
    public String toString() {
        return "DefaultLwM2MRegistrationEvent{" +
            "endpoint='" + endpoint + '\'' +
            ", registrationId='" + registrationId + '\'' +
            ", type=" + type +
            '}';
    }
}
