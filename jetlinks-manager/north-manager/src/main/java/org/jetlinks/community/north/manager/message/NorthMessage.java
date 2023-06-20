package org.jetlinks.community.north.manager.message;

import java.util.Map;

public class NorthMessage {

    /**
     * 消息唯一标识，可用于消息去重
     */
    private String     uid;

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

}
