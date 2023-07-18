package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpRequest;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.web.request.DeviceCommandRequest;
import org.springframework.web.bind.annotation.*;
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
@Authorize(ignore = true)
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
    public Flux<?> sendServiceContextCommand(@RequestBody DeviceCommandRequest cmdReq,
                                             @RequestHeader(name = "X-Access-Token") String xAccessToken) {
        if (!xAccessToken.equals("TEST123")) return Flux.empty();

        return service.invokeFunction(cmdReq.getDeviceId(), cmdReq.getFunctionId(), cmdReq.getFunctionParams());
    }

}
