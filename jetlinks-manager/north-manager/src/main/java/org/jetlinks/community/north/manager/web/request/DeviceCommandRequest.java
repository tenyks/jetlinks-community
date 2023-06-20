package org.jetlinks.community.north.manager.web.request;


import lombok.*;

import java.util.Map;

/**
 * @author tenyks
 * @since 1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class DeviceCommandRequest {

    /**
     * 产品标识
     */
    private String      productId;

    /**
     * 设备标识
     */
    private String      deviceId;

    /**
     * 指令有效时长，（单位：秒）
     */
    private Integer     ttlInSeconds;

    /**
     * 是否异步下发，默认异步下发，需要通讯协议和设备支持才能选择同步下发，否则下发指令时报错
     */
    private Boolean     async;

    /**
     * 物模型服务/功能标识
     */
    private String      functionId;

    /**
     * 物模型服务/功能参数，参数编码和参数类型需要与物模型定义的服务/功能参数一致
     */
    private Map<String, Object>     functionParams;

}
