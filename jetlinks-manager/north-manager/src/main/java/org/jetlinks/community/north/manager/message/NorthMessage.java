package org.jetlinks.community.north.manager.message;

import java.io.Serializable;
import java.util.Map;

public class NorthMessage implements Serializable {

    private static final long serialVersionUID = -8375903165168159488L;

    /**
     * 消息唯一标识，可用于消息去重
     */
    private String     uuid;

    /**
     * 产品标识
     */
    private String      productId;

    /**
     * 设备标识
     */
    private String      deviceId;

    /**
     * 设备名称
     */
    private String      deviceName;

    /**
     * OFFLINE = 离线
     * ONLINE = 上线
     * INVOKE_FUNCTION_REPLY = 调用功能/下发指令回复
     * REPORT_PROPERTY = 属性上报
     */
    private String      messageType;

    /**
     * 消息唯一标识，（可与下发指令返回的消息唯一标识来区分哪条指令的回复）
     */
    private String      messageId;

    /**
     * 发生时间
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
     * 指令执行结果：类型为相应服务/功能的输出参数
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
     * 上报的属性，编码与类型与物模型定义一致
     * 参考：产品物模型
     */
    private Map<String, Object> properties;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
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

    @Override
    public String toString() {
        return "NorthMessage{" +
            "uuid='" + uuid + '\'' +
            ", productId='" + productId + '\'' +
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
            ", properties=" + properties +
            '}';
    }
}
