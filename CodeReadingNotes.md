
# 事件相关
* EventBus
* DeviceMessageConnector
* 

## 事件消费
* ReactorQLTaskExecutorProvider
* SubscriberProvider
* SubscriptionProvider
* |-> RuleEngineSubscriptionProvider
* |-> NotificationsPublishProvider
* |-> DeviceStatusMeasurementProvider
* |-> DeviceMessageMeasurementProvider
* |-> DeviceMessageSubscriptionProvider
* org.jetlinks.community.gateway.annotation.Subscribe：标注
* org.jetlinks.community.gateway.spring.MessageListener

## 核心概念映射的类
* SubscribeRequest
* org.jetlinks.community.gateway.Subscription

## 框架
* org.jetlinks.core.utils.TopicUtils
* org.jetlinks.community.gateway.external.DefaultMessagingManager



### 数据流


# 关键流程
## 消息同步或异步下发
* org.jetlinks.core.device.DeviceMessageSender
* |-> org.jetlinks.core.defaults.DefaultDeviceMessageSender
* org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor
* org.jetlinks.core.message.Headers.async
* org.jetlinks.core.message.Headers.sendAndForget
* org.jetlinks.core.message.FunctionInvokeMessageSender
* org.jetlinks.core.server.MessageHandler
* |-> org.jetlinks.core.device.DeviceOperationBroker
* |--> org.jetlinks.core.device.StandaloneDeviceMessageBroker
* |--> org.jetlinks.supports.cluster.AbstractDeviceOperationBroker
* |---> org.jetlinks.supports.cluster.ClusterDeviceOperationBroker
* |---> org.jetlinks.supports.cluster.EventBusDeviceOperationBroker
* |---> org.jetlinks.supports.cluster.RpcDeviceOperationBroker


# 关键配置
* 消息响应超时：`jetlinks.device.message.default-timeout`，单位：秒