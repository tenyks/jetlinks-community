package org.jetlinks.community.north.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.community.north.manager.web.request.DeviceCommandRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author tenyks
 * @since 1.0
 */
@RestController
@RequestMapping("/openapi/north/device")
@Resource(id = "NorthDeviceOpenAPI", name = "北向接口-设备")
@Tag(name = "北向接口-设备")
public class DeviceOpenController {

    /**
     *
     * @param cmdReq
     * @return  指令的messageId
     */
    @PostMapping("/sendCommand")
    @SaveAction @Operation(summary = "下发指令")
    public Mono<String> sendCommand(@RequestBody DeviceCommandRequest cmdReq) {
        return null;
    }

}
