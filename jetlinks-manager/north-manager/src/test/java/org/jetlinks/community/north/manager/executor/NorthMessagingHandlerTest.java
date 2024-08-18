package org.jetlinks.community.north.manager.executor;

import org.elasticsearch.core.Map;
import org.elasticsearch.core.Tuple;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class NorthMessagingHandlerTest {


    private NorthMessagingHandler handler;

    private EventBus    eventBus;

    @Before
    public void init() {
        String jmsUrl = "tcp://MyDevS1:61616";

        eventBus = new BrokerEventBus();
        handler = new NorthMessagingHandler(jmsUrl, "/unitTest/north/message", null, null);
    }

    @Test
    public void createTask() throws Exception {
        handler.afterPropertiesSet();

        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < 100; i++) {
            Thread.sleep(rand.nextInt(30));

            Tuple<String, Message> tuple = createTestMessage(i);
            System.out.printf("====[%s]%s%n", tuple.v1(), tuple.v2().toJson());
            eventBus.publish(tuple.v1(), tuple.v2()).subscribe();
        }

        Thread.sleep(60000);
    }

    private Tuple<String, Message> createTestMessage(int flag) {
        Tuple<String, Message> rst;
        switch (flag % 10) {
            case 0: {
                ReportPropertyMessage msg = ReportPropertyMessage.create();
                msg.setMessageId("MID-00001");
                msg.setCode("SUCCESS");
                msg.setDeviceId("DEV-1001");
                msg.setTimestamp(System.currentTimeMillis());
                msg.properties(Map.of("p1", flag, "p2", "Hello"));

                rst = Tuple.tuple("/message/property/report", msg);
                break;
            }
            case 1: {
                ReadPropertyMessageReply msg = ReadPropertyMessageReply.create();
                msg.setMessageId("MID-00001");
                msg.setCode("SUCCESS");
                msg.setDeviceId("DEV-1001");
                msg.setTimestamp(System.currentTimeMillis());
                msg.properties(Map.of("p1", flag, "p2", "Hello"));

                rst = Tuple.tuple("/message/property/read/reply", msg);
                break;
            }
            case 2: {
                WritePropertyMessageReply msg = WritePropertyMessageReply.create();
                msg.setMessageId("MID-00001");
                msg.setCode("SUCCESS");
                msg.setDeviceId("DEV-1001");
                msg.setTimestamp(System.currentTimeMillis());
                msg.properties(Map.of("p1", flag, "p2", "Hello"));

                rst = Tuple.tuple("/message/property/write/reply", msg);
                break;
            }
            case 3: {
                FunctionInvokeMessageReply msg = FunctionInvokeMessageReply.create();
                msg.setMessageId("MID-00001");
                msg.setCode("SUCCESS");
                msg.setDeviceId("DEV-1001");
                msg.setTimestamp(System.currentTimeMillis());
                msg.setFunctionId("commandXxx");
                msg.setOutput(Map.of("p1", flag, "p2", "Hello"));

                rst = Tuple.tuple("/message/function/reply", msg);
                break;
            }
            case 4: {
                DeviceRegisterMessage msg = new DeviceRegisterMessage();
                msg.setMessageId("MID-00001");
                msg.setCode("SUCCESS");
                msg.setDeviceId("DEV-1001");
                msg.setTimestamp(System.currentTimeMillis());

                rst = Tuple.tuple("/register", msg);
                break;
            }
            case 5: {
                DeviceRegisterMessage msg = new DeviceRegisterMessage();
                msg.setMessageId("MID-00001");
                msg.setCode("SUCCESS");
                msg.setDeviceId("DEV-1001");
                msg.setTimestamp(System.currentTimeMillis());

                rst = Tuple.tuple("/unregister", msg);
                break;
            }
            case 6: {
                DeviceLogMessage msg = new DeviceLogMessage();
                msg.setMessageId("MID-00001");
                msg.setCode("SUCCESS");
                msg.setDeviceId("DEV-1001");
                msg.setTimestamp(System.currentTimeMillis());
                msg.setLog("[Log]Hello World!");

                rst = Tuple.tuple("/message/log", msg);
                break;
            }
            case 7: {
                DirectDeviceMessage msg = new DirectDeviceMessage();
                msg.setMessageId("MID-00001");
                msg.setCode("SUCCESS");
                msg.setDeviceId("DEV-1001");
                msg.setTimestamp(System.currentTimeMillis());
                msg.setPayload(String.format("HelloWorld%d!", flag).getBytes(StandardCharsets.UTF_8));

                rst = Tuple.tuple("/message/direct", msg);
                break;
            }
            case 8: {
                DeviceOnlineMessage msg = new DeviceOnlineMessage();
                msg.setMessageId("MID-00001");
                msg.setCode("SUCCESS");
                msg.setDeviceId("DEV-1001");
                msg.setTimestamp(System.currentTimeMillis());

                rst = Tuple.tuple("/online", msg);
                break;
            }
            default: {
                DeviceOnlineMessage msg = new DeviceOnlineMessage();
                msg.setMessageId("MID-00001");
                msg.setCode("SUCCESS");
                msg.setDeviceId("DEV-1001");
                msg.setTimestamp(System.currentTimeMillis());

                rst = Tuple.tuple("/offline", msg);
                break;
            }
        }

        return rst;
    }

}