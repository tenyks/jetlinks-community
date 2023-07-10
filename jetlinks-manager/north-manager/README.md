# north-manager | 北向对接管理

## 功能概述
* 提供北向开发接口；
* 提供北向推送的规则Action；


## 北向对接指引

### 北向接口清单
* 下发设备指令，请参考：org.jetlinks.community.device.web.DeviceOpenController.sendCommand

### 北向推送
* 由物联网平台提供JMS队列，北向系统实现消息的消费和业务处理，QoS=最少一次
* 消息负载为JSON字符串，消息结构参考org.jetlinks.community.north.manager.message.NorthMessage

