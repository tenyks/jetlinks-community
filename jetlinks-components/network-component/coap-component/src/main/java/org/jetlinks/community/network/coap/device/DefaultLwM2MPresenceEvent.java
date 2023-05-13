package org.jetlinks.community.network.coap.device;

import org.eclipse.leshan.server.registration.Registration;
import org.jetlinks.community.network.coap.server.lwm2m.LwM2MPresenceEvent;

public class DefaultLwM2MPresenceEvent implements LwM2MPresenceEvent {

    private final Registration registration;

    private final boolean   ofAwake;

    public DefaultLwM2MPresenceEvent(Registration registration, boolean ofAwake) {
        this.registration = registration;
        this.ofAwake = ofAwake;
    }

    @Override
    public Registration getRegistration() {
        return registration;
    }

    @Override
    public boolean isOfAwake() {
        return ofAwake;
    }

    @Override
    public String toString() {
        return "DefaultLwM2MPresenceEvent{" +
            "registration=" + registration +
            ", ofAwake=" + ofAwake +
            '}';
    }
}
