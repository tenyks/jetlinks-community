package org.jetlinks.community.north.manager.message;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;

import java.io.Serializable;
import java.util.Map;

public class NorthMessage implements Serializable {

    private static final long serialVersionUID = -8375903165168159488L;

    /**
     * 消息唯一标识，可用于消息去重
     */
    private String      uuid;

    /**
     * 物模型编码
     */
    private String      thingId;

    /**
     * 设备标识
     */
    private String      deviceId;

    /**
     * 设备名称
     */
    private String      deviceName;

    /**
     * 消息类型
     * DEVICE_OFFLINE = 设备离线事件
     * DEVICE_ONLINE = 设备上线事件
     * INVOKE_FUNCTION_REPLY = 调用功能/下发指令回复
     * REPORT_PROPERTY = 属性上报
     * REPORT_EVENT = 事件上报
     */
    private String      messageType;

    /**
     * 消息唯一标识，（可与下发指令返回的消息唯一标识来区分哪条指令的回复）
     */
    private String      messageId;

    /**
     * 发生时间戳，单位：毫秒
     */
    private Long        timestamp;

    /**
     * 指令下发结果编码
     * REQUEST_HANDLING = 请求处理中
     */
    private String      code;

    /**
     * 指令下发处理是否成功
     */
    private Boolean     success;

    /**
     * 指令执行结果：类型为相应服务/功能/的输出参数或物模型事件的参数
     * 参考：产品物模型
     */
    private Object      output;

    /**
     * 指令处理提示
     */
    private String      message;

    /**
     * 相关的物模型服务/功能标识
     * 参考：产品物模型
     */
    private String      functionId;

    /**
     * 相关的物模型事件标识
     * 参考：产品物模型
     */
    private String      eventId;

    /**
     * 上报的属性，编码与类型与物模型定义一致
     * 参考：产品物模型
     */
    private Map<String, Object> properties;

    /**
     * 原始消息的JSON字符串
     */
    private String      rawMessage;

    public static NorthMessage fromMessage(DeviceMessage msg) {
        if (msg instanceof ReportPropertyMessage) {
            return fromMessage((ReportPropertyMessage) msg);
        } else if (msg instanceof ReadPropertyMessageReply) {
            return fromMessage((ReadPropertyMessageReply) msg);
        } else if (msg instanceof WritePropertyMessageReply) {
            return fromMessage((WritePropertyMessageReply) msg);
        } else if (msg instanceof DeviceRegisterMessage) {
            return fromMessage((DeviceRegisterMessage) msg);
        } else if (msg instanceof DeviceUnRegisterMessage) {
            return fromMessage((DeviceUnRegisterMessage) msg);
        } else if (msg instanceof DeviceLogMessage) {
            return fromMessage((DeviceLogMessage) msg);
        } else if (msg instanceof DeviceOnlineMessage) {
            return fromMessage((DeviceOnlineMessage) msg);
        } else if (msg instanceof DeviceOfflineMessage) {
            return fromMessage((DeviceOfflineMessage) msg);
        } else if (msg instanceof DirectDeviceMessage) {
            return fromMessage((DirectDeviceMessage) msg);
        } else if (msg instanceof FunctionInvokeMessageReply) {
            return fromMessage((FunctionInvokeMessageReply) msg);
        } else if (msg instanceof EventMessage) {
            return fromMessage((EventMessage) msg);
        }

        return _buildDefault(msg);
    }

    private static NorthMessage _buildDefault(DeviceMessage msg) {
        NorthMessage rstMsg = new NorthMessage();

        rstMsg.setUuid(msg.getMessageId());
        rstMsg.setMessageId(msg.getMessageId());
        rstMsg.setDeviceId(msg.getDeviceId());
        rstMsg.setThingId(msg.getThingId());
        rstMsg.setMessageType(msg.getMessageType().name());
        rstMsg.setTimestamp(msg.getTimestamp());

        rstMsg.setRawMessage(JSONObject.toJSONString(msg));

        return rstMsg;
    }

    public static NorthMessage fromMessage(ReportPropertyMessage msg) {
        NorthMessage rstMsg = new NorthMessage();

        rstMsg.setUuid(msg.getMessageId());
        rstMsg.setMessageId(msg.getMessageId());
        rstMsg.setDeviceId(msg.getDeviceId());
        rstMsg.setThingId(msg.getThingId());
        rstMsg.setProperties(msg.getProperties());
        rstMsg.setMessageType("REPORT_PROPERTY");
        rstMsg.setTimestamp(msg.getTimestamp());

        rstMsg.setRawMessage(JSONObject.toJSONString(msg));

        return rstMsg;
    }

    public static NorthMessage fromMessage(ReadPropertyMessageReply msg) {
        return null;
    }

    public static NorthMessage fromMessage(WritePropertyMessageReply msg) {
        return null;
    }

    public static NorthMessage fromMessage(DeviceRegisterMessage msg) {
        return null;
    }

    public static NorthMessage fromMessage(DeviceUnRegisterMessage msg) {
        return null;
    }

    public static NorthMessage fromMessage(DeviceLogMessage msg) {
        return null;
    }

    public static NorthMessage fromMessage(DeviceOnlineMessage msg) {
        NorthMessage rstMsg = new NorthMessage();

        rstMsg.setUuid(msg.getMessageId());
        rstMsg.setMessageId(msg.getMessageId());
        rstMsg.setDeviceId(msg.getDeviceId());
        rstMsg.setThingId(msg.getThingId());
        rstMsg.setMessageType("DEVICE_ONLINE");
        rstMsg.setTimestamp(msg.getTimestamp());

        rstMsg.setRawMessage(JSONObject.toJSONString(msg));

        return rstMsg;
    }

    public static NorthMessage fromMessage(DeviceOfflineMessage msg) {
        NorthMessage rstMsg = new NorthMessage();

        rstMsg.setUuid(msg.getMessageId());
        rstMsg.setMessageId(msg.getMessageId());
        rstMsg.setDeviceId(msg.getDeviceId());
        rstMsg.setThingId(msg.getThingId());
        rstMsg.setMessageType("DEVICE_OFFLINE");
        rstMsg.setTimestamp(msg.getTimestamp());

        rstMsg.setRawMessage(JSONObject.toJSONString(msg));

        return rstMsg;
    }

    public static NorthMessage fromMessage(DirectDeviceMessage msg) {
        return null;
    }

    public static NorthMessage fromMessage(FunctionInvokeMessageReply msg) {
        NorthMessage rstMsg = new NorthMessage();

        rstMsg.setUuid(msg.getMessageId());
        rstMsg.setMessageId(msg.getMessageId());
        rstMsg.setDeviceId(msg.getDeviceId());
        rstMsg.setThingId(msg.getThingId());
        rstMsg.setFunctionId(msg.getFunctionId());
        rstMsg.setMessageType("INVOKE_FUNCTION_REPLY");
        rstMsg.setOutput(msg.getOutput());
        rstMsg.setSuccess(msg.isSuccess());
        rstMsg.setTimestamp(msg.getTimestamp());

        rstMsg.setRawMessage(JSONObject.toJSONString(msg));

        return rstMsg;
    }

    public static NorthMessage fromMessage(EventMessage msg) {
        NorthMessage rstMsg = new NorthMessage();

        rstMsg.setUuid(msg.getMessageId());
        rstMsg.setMessageId(msg.getMessageId());
        rstMsg.setDeviceId(msg.getDeviceId());
        rstMsg.setThingId(msg.getThingId());
        rstMsg.setEventId(msg.getEvent());
        rstMsg.setMessageType("REPORT_EVENT");
        rstMsg.setOutput(msg.getData());
        rstMsg.setTimestamp(msg.getTimestamp());

        rstMsg.setRawMessage(JSONObject.toJSONString(msg));

        return rstMsg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFunctionId() {
        return functionId;
    }

    public void setFunctionId(String functionId) {
        this.functionId = functionId;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public String toString() {
        return "NorthMessage{" +
            "uuid='" + uuid + '\'' +
            ", thingId='" + thingId + '\'' +
            ", deviceId='" + deviceId + '\'' +
            ", deviceName='" + deviceName + '\'' +
            ", messageType='" + messageType + '\'' +
            ", messageId='" + messageId + '\'' +
            ", timestamp=" + timestamp +
            ", code='" + code + '\'' +
            ", success=" + success +
            ", output=" + output +
            ", message='" + message + '\'' +
            ", functionId='" + functionId + '\'' +
            ", eventId='" + eventId + '\'' +
            ", properties=" + properties +
            ", rawMessage='" + rawMessage + '\'' +
            '}';
    }
}
