package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.web.request.DeviceCommandRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author tenyks
 * @since 1.0
 */
@RestController
@RequestMapping("/openapi/north/device")
@Resource(id = "NorthDeviceOpenAPI", name = "北向接口-设备")
@Tag(name = "北向接口-设备")
@Slf4j
public class DeviceOpenController {

    @Getter
    private final LocalDeviceInstanceService service;

    public DeviceOpenController(LocalDeviceInstanceService service) {
        this.service = service;
    }

    /**
     *
     * @param cmdReq
     * @return  指令的messageId
     */
    @PostMapping("/sendCommand")
    @SaveAction @Operation(summary = "下发指令")
    public Flux<String> sendServiceContextCommand(@RequestBody DeviceCommandRequest cmdReq) {
        service.invokeFunction(cmdReq.getDeviceId(), cmdReq.getFunctionId(), cmdReq.getFunctionParams());

        return null;
    }

}
