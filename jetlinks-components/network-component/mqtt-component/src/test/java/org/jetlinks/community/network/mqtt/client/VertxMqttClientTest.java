package org.jetlinks.community.network.mqtt.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import org.jetlinks.core.message.codec.MessagePayloadType;
import org.jetlinks.core.message.codec.mqtt.SimpleMqttMessage;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class VertxMqttClientTest {

    private VertxMqttClient client;

    @Before
    public void init() {
        client = new VertxMqttClient("TEST");

        MqttClient mqttClient = MqttClient.create(Vertx.vertx());
        mqttClient.connect(21883, "MyDevS1");
        client.setClient(mqttClient);
    }


    @Test
    public void publish() {
        ByteBuf payload = Unpooled.copiedBuffer("{\"key1\":1}", StandardCharsets.UTF_8);
        SimpleMqttMessage message = new SimpleMqttMessage("/demo/device1", "iotbase", 1, payload, 100, false, true, false, MessagePayloadType.JSON);

        Mono<Void> rst = client.publish(message);
        rst.subscribe();
    }

    @Test
    public void subscribe() {

        client.subscribe(Collections.singletonList("/demo/device1")).subscribe(v -> {
            System.out.println(v.print());
        });
    }
}