# cloud-demo 微服务学习项目 · 设计文档

> 日期：2026-08-03
> 作者：liang（王梁）& ZCode 导师
> 状态：✅ 已获用户批准

---

## 一、项目愿景

将现有空壳项目 `cloud-demo` 重构为**教科书式结构**的微服务学习项目。
以"网店商品库存"为业务主线，让 liang 通过亲手编写代码，系统学习微服务的
核心概念：服务注册发现、配置中心、远程调用、网关、限流治理。

配套课程目录 `microservice-course/` 提供 12 章从零到实战的学习笔记。

---

## 二、技术栈

| 技术 | 版本 | 作用 |
|------|------|------|
| Java | 21（本机已装） | 编程语言 |
| Spring Boot | 3.3.4（已有） | 应用框架 |
| Spring Cloud | 2023.0.3（已有） | 微服务全家桶 |
| Spring Cloud Alibaba | 2023.0.3.2（已有） | 阿里微服务组件 |
| Nacos | 2.x（Docker） | 注册中心 + 配置中心 |
| OpenFeign | 随 Spring Cloud | 服务间远程调用 |
| MyBatis-Plus | 3.5.x | 数据库 ORM |
| MySQL | 9.x（Docker） | 数据存储 |
| Gateway | 随 Spring Cloud | API 网关 |
| Sentinel | 随 Alibaba | 限流熔断（第 10-11 章） |

### 环境端口清单（重要）

| 组件 | 端口 | 访问方 | 说明 |
|------|------|--------|------|
| Nacos 控制台 | 18080 | 浏览器 | 登录 nacos/nacos |
| Nacos 服务端 | 8848 | 程序 | 服务注册/配置 API |
| Nacos gRPC | 9848/9849 | 程序 | Nacos 2.x 通信 |
| MySQL | 3306 | 程序 | root/123456 |

---

## 三、目标项目结构（教科书式）

```
cloud-demo/                          # 父工程：统一依赖版本，不写业务
├── pom.xml                          # 父 pom：所有版本集中管理
├── common/
│   └── common-core/                 # 公共模块：统一返回、错误码、工具类
├── services/
│   ├── service-product/             # 📦 商品服务（核心学习对象）
│   └── service-stock/               # 📦 库存服务（被商品服务 Feign 调用）
├── gateway/                         # 🚪 API 网关（第 10-11 章加入）
└── docs/superpowers/specs/          # 设计文档
```

### 公共模块 common-core 内容

- `R<T>`：统一返回体（code + msg + data）
- `ResultCode`：错误码枚举（沿用项目 700xx 规范）
- `GlobalExceptionHandler`：全局异常处理
- 工具类：如 `SnowflakeIdGenerator`（雪花 ID 生成）

### 各服务职责

| 服务 | 端口 | 数据库 | 职责 |
|------|------|--------|------|
| service-product | 8081 | product_db | 商品增删改查（MyBatis-Plus） |
| service-stock | 8082 | stock_db | 库存查询、扣减 |
| gateway（后期） | 8080 | - | 统一入口、路由转发 |

---

## 四、业务主线（学习场景）

> 你管理一家网店：
> 1. 顾客查看商品列表 → **商品服务**（查 product_db）
> 2. 顾客下单时 → 商品服务通过 **Feign** 调用 **库存服务** 查询库存
> 3. 下单成功 → 调用库存服务扣减库存（演示服务间通信）
> 4. 未来：所有请求从 **Gateway 网关** 统一进入

### 数据表设计

**product_db 商品库**
```sql
CREATE TABLE product (
    id          BIGINT PRIMARY KEY,        -- 雪花 ID
    name        VARCHAR(100) NOT NULL,     -- 商品名
    price       DECIMAL(10,2) NOT NULL,    -- 价格
    description VARCHAR(500),              -- 描述
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**stock_db 库存库**
```sql
CREATE TABLE stock (
    id          BIGINT PRIMARY KEY,        -- 雪花 ID
    product_id  BIGINT NOT NULL UNIQUE,    -- 关联商品
    quantity    INT NOT NULL,              -- 库存数量
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 五、课程目录设计（microservice-course/）

```
microservice-course/
├── 00-环境准备/          # 第 01 章：Docker + Nacos + MySQL（已完成✅）
├── 01-项目骨架/          # 第 02-03 章：父工程 + common + 双服务框架
├── 02-服务注册与发现/     # 第 04-05 章：Nacos 注册 + 配置中心
├── 03-服务间通信/        # 第 06-07 章：OpenFeign + 负载均衡
├── 04-数据访问/          # 第 08-09 章：MyBatis-Plus 连接数据库
├── 05-网关与治理/        # 第 10-11 章：Gateway + Sentinel
├── 06-进阶/             # 第 12 章+：链路追踪、Seata 事务（后期）
└── 07-项目实战/          # 🎓 毕业项目：商品下单全流程
```

**学习方法**：每完成一个功能 → 写一篇 Markdown 笔记（含思考过程、踩坑记录、代码讲解）→ 一次 git 提交。

---

## 六、开发阶段（分阶段 git 提交）

| 阶段 | 内容 | 章节 |
|------|------|------|
| P1 | 重构父工程 + 创建 common-core | 02 |
| P2 | 创建 service-product + service-stock 骨架，注册到 Nacos | 03-04 |
| P3 | 两个服务接入 MySQL（MyBatis-Plus） | 08 |
| P4 | 商品服务 Feign 调用库存服务 | 06 |
| P5 | Nacos 配置中心（配置文件外置） | 05 |
| P6 | Gateway 网关 | 10 |
| P7 | Sentinel 限流 | 11 |
| P8 | （后期）链路追踪 + Seata | 12+ |

---

## 七、成功标准

1. ✅ 两个服务能同时启动，在 Nacos 控制台看到 2 个注册实例
2. ✅ 商品服务能通过 Feign 调用库存服务，返回库存数据
3. ✅ 每章配一篇学习笔记，liang 能看着笔记复述概念
4. ✅ liang 能独立完成"商品下单扣库存"全流程演示
