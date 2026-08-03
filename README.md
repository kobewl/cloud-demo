# cloud-demo

📚 **微服务学习项目** —— 以"网店商品库存"为主线，系统学习 Spring Cloud Alibaba 微服务全家桶。

## ✨ 项目愿景

从一个空壳项目重构为**教科书式结构**的微服务学习项目，通过亲手编写代码，
学习微服务的核心概念：**服务注册发现、配置中心、远程调用、网关、流量治理**。
配套课程笔记见 [microservice-course](../microservice-course/)。

## 🚀 功能列表

### ✅ 已完成
- [x] 环境搭建：Docker 部署 Nacos（注册/配置中心）+ MySQL
- [x] 设计文档：`docs/superpowers/specs/`
- [x] P1 父工程重构 + common-core 公共模块（统一返回 R、错误码、全局异常）
- [x] P2 商品服务 + 库存服务骨架，注册到 Nacos
- [x] P3 接入 MySQL（MyBatis-Plus）
- [x] P4 商品服务 Feign 调用库存服务（详情带库存 + 购买扣库存）

### 🔨 规划中（按学习顺序）
- [ ] P5 Nacos 配置中心
- [ ] P6 Gateway 网关
- [ ] P7 Sentinel 限流
- [ ] P8 链路追踪 + Seata 事务（后期）

## 🏗️ 项目结构

```
cloud-demo/                          # 父工程：统一依赖版本，不写业务
├── pom.xml                          # 父 pom：所有版本集中管理
├── common/
│   └── common-core/                 # 公共模块：统一返回、错误码、工具类
├── services/
│   ├── service-product/             # 📦 商品服务（核心学习对象）
│   └── service-stock/               # 📦 库存服务（被商品服务 Feign 调用）
├── gateway/                         # 🚪 API 网关（规划中）
└── docs/superpowers/specs/          # 设计文档
```

## 🔧 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | 本机已装 |
| Maven | 3.9+ | 本机已装 |
| Docker | Desktop | 运行 Nacos / MySQL |

### 基础设施（Docker）

| 组件 | 端口 | 账号 | 说明 |
|------|------|------|------|
| Nacos 控制台 | 18080 | nacos/nacos | 浏览器访问 |
| Nacos 服务端 | 8848 | - | 程序注册/配置 |
| MySQL | 3306 | root/123456 | product_db / stock_db |

> Nacos 启动命令：`docker start nacos`（数据已持久化到数据卷）

## 📖 使用指南

```bash
# 1. 启动基础设施（Docker Desktop 需先运行）
docker start nacos mysql

# 2. 编译整个项目
mvn clean compile

# 3. 依次启动服务（每个服务一个终端）
mvn -pl services/service-product spring-boot:run
mvn -pl services/service-stock spring-boot:run

# 4. 浏览器打开 Nacos 控制台，观察服务注册
http://localhost:18080/
```

## 🧩 组件库

> 随代码完成逐步补充：`R` 统一返回、`ResultCode` 错误码、雪花 ID 工具等。

## 📚 学习进度

- [x] 第 01 章：环境准备（Docker + Nacos + MySQL）
- [ ] 第 02-03 章：项目骨架
- [ ] 第 04-05 章：服务注册与配置中心
- [ ] 第 06-07 章：服务间通信
- [ ] 第 08-09 章：数据访问
- [ ] 第 10-11 章：网关与治理
- [ ] 第 12 章+：进阶
