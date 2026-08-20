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
- [x] P5 Nacos 配置中心（配置文件外置 + 动态刷新）
- [x] P6 Gateway 网关（统一入口 + 路由转发 + 路由配置外置 Nacos 动态刷新）
- [x] P7 Sentinel 限流（网关按 route 限流：order 2 QPS / product 5 QPS，超限返回 429）
- [x] P7+ Sentinel 限流规则外置 Nacos 数据源（`gateway-flow.json` 动态热更新，改 QPS 不重启网关，秒级生效）
- [x] 订单服务 service-order（下单：Feign 查商品 → 扣库存 → 落订单，失败补偿回补库存）
- [x] P8 Seata 分布式事务（`@GlobalTransactional` 自动回滚，替换手写补偿）
- [x] 前端控制台 frontend（Vue 3 + Element Plus：商品管理、配库存、下单、订单列表）
- [x] P9 链路追踪（Micrometer Tracing + Zipkin，看网关→订单→商品→库存完整调用链）

## 🏗️ 项目结构

```
cloud-demo/                          # 父工程：统一依赖版本，不写业务
├── pom.xml                          # 父 pom：所有版本集中管理
├── common/
│   └── common-core/                 # 公共模块：统一返回、错误码、工具类
├── services/
│   ├── service-product/             # 📦 商品服务（核心学习对象）
│   ├── service-stock/               # 📦 库存服务（被商品服务 Feign 调用）
│   └── service-order/               # 📦 订单服务（下单扣库存，跨服务调用）
├── gateway/                         # 🚪 API 网关（统一入口 + 路由转发）
├── frontend/                        # 🖥️ 前端控制台（Vue 3 + Element Plus）
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
| Zipkin | 9411 | - | 链路追踪 UI（P9） |

> Nacos 启动命令：`docker start nacos`（数据已持久化到数据卷）
> Zipkin 启动命令：`docker start zipkin`（P9 链路追踪）

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

## 🖥️ 前端控制台（frontend/）

用 Vue 3 + TypeScript + Vite + Element Plus 搭建的操作界面，可视化操作微服务的完整链路。

### 功能
- 📦 **商品列表**：查看、新增、删除商品
- 🔍 **商品详情**：查看商品 + 当前库存，一键下单（走 Seata 分布式事务）
- 🗃️ **配库存**：给商品初始化库存（一个商品只能配一条）
- 🧾 **订单列表**：查看所有订单（下单成功后自动出现）
- 📢 **顶栏公告**：展示 Nacos 配置中心的 `shop.notice`，演示"改配置不重启、动态刷新"

### 启动
```bash
# 1. 先启动后端（网关 + 三个业务服务，见上面的"使用指南"）

# 2. 进入前端目录，安装依赖（首次）
cd frontend
npm install

# 3. 启动开发服务器（Vite 会把 /api 代理到网关 8080）
npm run dev
# 浏览器打开 http://localhost:5173
```

### 常用命令
```bash
npm run dev         # 启动开发服务器（带热更新）
npm run build       # 生产构建（输出到 frontend/dist/）
npm run type-check  # 类型检查（vue-tsc）
```

### 技术要点
- **跨域方案**：开发时用 Vite 的 `server.proxy` 把 `/api` 请求转发到网关 `8080`，
  前端与后端"同源"，无需后端开 CORS（`vite.config.ts`）。
- **统一返回解包**：后端返回 `R{ code, msg, data }`，前端在 `src/api/request.ts`
  用 Axios 拦截器统一解包——成功返回 `data`，失败弹 `msg`，全站只写一次。
- **雪花 ID 精度**：后端雪花算法生成的 19 位 ID 超出 JS `number` 精确范围，
  已在 `common-core` 加 Jackson 配置把 `Long` 序列化成字符串，
  前端 ID 统一用 `string`，避免删除/查询时"张冠李戴"。
- **分层结构**：`api/`（接口封装）→ `stores/`（Pinia 状态）→ `views/`（页面），
  职责单一、符合 SOLID。

## ⚙️ 配置中心说明（P5）

- 每个服务的**业务/环境配置**（数据库连接、MyBatis-Plus、自定义项）已外置到 Nacos，
  本地 `application.yml` 只保留端口、服务名、Nacos 地址等启动必需项。
- 所有配置集中在 **`cloud-demo` 分组**（Group），不再使用默认分组：
  `application.yml` 里通过 `spring.cloud.nacos.config.group: cloud-demo` 指定，
  服务注册/发现同样用 `spring.cloud.nacos.discovery.group: cloud-demo`。
- 配置的"源文件"在 `docs/nacos-config/`（配置即代码）：
  - `service-product.yaml` → 商品服务（Data ID: `service-product.yaml`, Group: `cloud-demo`）
  - `service-stock.yaml` → 库存服务（Data ID: `service-stock.yaml`, Group: `cloud-demo`）
  - `service-order.yaml` → 订单服务（Data ID: `service-order.yaml`, Group: `cloud-demo`）
  - `gateway.yaml` → 网关（Data ID: `gateway.yaml`, Group: `cloud-demo`）
- **改配置流程**：编辑 `docs/nacos-config/` 里的源文件 → 同步到 Nacos 控制台
  （配置管理 → 配置列表 → 选择 `cloud-demo` 分组 → 编辑 → 发布）→ 服务通过 `@RefreshScope` **无需重启**即可生效。
- 验证动态刷新：商品服务 `GET /api/config/notice` 返回 `shop.notice` 配置，
  改掉 Nacos 里该值并发布，再访问接口即可看到新值。

> ⚠️ 版本说明：Spring Cloud Alibaba 2023.0.1.3+ 已废弃 bootstrap 方式，
> 本项目用 `spring.config.import: optional:nacos:xxx.yaml?refreshEnabled=true` 接入
> （与本项目 SCA 2023.0.3.2 / Nacos 3.x 匹配）。

## 🧩 组件库

> 随代码完成逐步补充：`R` 统一返回、`ResultCode` 错误码、雪花 ID 工具等。

## 📚 学习进度

- [x] 第 01 章：环境准备（Docker + Nacos + MySQL）
- [x] 第 02-03 章：项目骨架
- [x] 第 04-05 章：服务注册与配置中心
- [x] 第 06-07 章：服务间通信
- [x] 第 08-09 章：数据访问
- [x] 第 10-11 章：网关与治理（Gateway 路由 + Sentinel 限流）
- [ ] 第 12 章+：进阶
