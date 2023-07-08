# JetLinks 物联网基础平台

JetLinks 基于Java8,Spring Boot 2.x,WebFlux,Netty,Vert.x,Reactor等开发, 
是一个开箱即用,可二次开发的企业级物联网基础平台。平台实现了物联网相关的众多基础功能,
能帮助你快速建立物联网相关业务系统。
 

## 核心特性

#### 开放源代码

全部源代码开放,可自由拓展功能,不再受制于人.前后端分离,接口全开放。

#### 统一设备接入,海量设备管理
TCP/UDP/MQTT/HTTP、TLS/DTLS、不同厂商、不同设备、不同报文、统一接入，统一管理。

#### 规则引擎
灵活的规则模型配置,支持多种规则模型以及自定义规则模型. 设备告警,场景联动,均由统一的规则引擎管理。

#### 数据权限控制
灵活的非侵入数据权限控制。可实现菜单、按钮、数据三维维度的数据权限控制。可控制单条数据的操作权限。

## 技术栈

1. [Spring Boot 2.7.x](https://spring.io/projects/spring-boot)
2. [Spring WebFlux](https://spring.io/) 响应式Web支持
3. [R2DBC](https://r2dbc.io/) 响应式关系型数据库驱动
4. [Project Reactor](https://projectreactor.io/) 响应式编程框架
4. [Netty](https://netty.io/),[Vert.x](https://vertx.io/) 高性能网络编程框架
5. [ElasticSearch](https://www.elastic.co/cn/products/enterprise-search) 全文检索，日志，时序数据存储
6. [PostgreSQL](https://www.postgresql.org) 业务功能数据管理
7. [hsweb framework 4](https://github.com/hs-web) 业务功能基础框架

## 架构

![platform](./platform.png)

## 设备接入流程

![device-flow](./device-flow.png)

## 模块

```bash
--jetlinks-community
------|----docker
------|------|----dev-env       # 启动开发环境
------|------|----run-all       # 启动全部,通过http://localhost:9000 访问系统.
------|----jetlinks-components  # 公共组件模块
------|----jetlinks-manager     # 业务管理模块
------|----jetlinks-standalone  # 服务启动模块
------|----simulator            # 设备模拟器
```

## 配置项
* jetlinks.device.message.default-timeout：消息下发超时

## 文档

[快速开始](http://doc.jetlinks.cn/install-deployment/start-with-source.html) 
[开发文档](http://doc.jetlinks.cn/dev-guide/start.html) 
[常见问题](http://doc.jetlinks.cn/common-problems/install.html) 
