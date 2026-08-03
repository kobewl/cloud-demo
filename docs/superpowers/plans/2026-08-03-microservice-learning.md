# cloud-demo 微服务学习项目 · 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把空壳 cloud-demo 重构为教科书式微服务项目：公共模块 + 商品服务 + 库存服务，实现 Nacos 注册发现、MySQL 数据访问、Feign 远程调用，跑通"商品详情带库存"业务主线。

**Architecture:** 父工程统一依赖版本 → `common/common-core` 提供统一返回 R、错误码、全局异常处理 → `services/service-product`（8081，product_db）与 `services/service-stock`（8082，stock_db）注册到 Nacos → 商品服务通过 OpenFeign 调用库存服务查询库存。

**Tech Stack:** Spring Boot 3.3.4 / Spring Cloud 2023.0.3 / Spring Cloud Alibaba 2023.0.3.2 / Nacos 2.x / OpenFeign / MyBatis-Plus 3.5.7 / MySQL 9.x

## Global Constraints

- 所有错误码在 `ResultCode` 枚举中定义（700xx 段），禁止代码里写裸错误码；调用处 `R.fail(ResultCode.XXX)`
- Feign 调用失败透传原始 code/msg，禁止用通用提示替换
- 包结构统一前缀 `com.wangliang.cloud`；所有注释用简体中文
- 两个服务的启动类必须 `@SpringBootApplication(scanBasePackages = "com.wangliang.cloud")`，否则扫不到 common-core 里的 `GlobalExceptionHandler`
- 每个任务结束后一次 git 提交
- Java 17 编译目标（本机 JDK 21 兼容）
- 环境：Nacos 服务端 127.0.0.1:8848（账号 nacos/nacos）、MySQL 127.0.0.1:3306（root/123456，库 product_db、stock_db）

---

### Task 1: 重构聚合父工程（模块划分）

**Files:**
- Modify: `pom.xml`（根，modules 增加 common）
- Modify: `services/pom.xml`（modules 用 service-stock 替换 service-order，删除多余属性）
- Create: `common/pom.xml`
- Create: `common/common-core/pom.xml`

**Interfaces:**
- Consumes: 无
- Produces: 聚合模块 `common`（父 pom）与 `common-core`（jar 模块），供 Task 2 使用；父 pom 新增 `mybatis-plus.version=3.5.7` 属性供 Task 3/4 使用

- [ ] **Step 1: 修改根 pom.xml 的 modules**

编辑 `/Users/wangliang/Documents/JavaProject/cloud-demo/pom.xml`，把 `<modules>` 改为：

```xml
    <modules>
        <module>common</module>
        <module>services</module>
    </modules>
```

并在 `<properties>` 中增加两行（放在 `<project.build.sourceEncoding>` 之后）：

```xml
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
```

- [ ] **Step 2: 修改 services/pom.xml**

编辑 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/pom.xml`：

把 `<modules>` 改为：

```xml
    <modules>
        <module>service-product</module>
        <module>service-stock</module>
    </modules>
```

删除整个 `<properties>` 块（父工程已统一 17，不再覆盖）：

```xml
    <properties>
        <maven.compiler.source>19</maven.compiler.source>
        <maven.compiler.target>19</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
```

保留 services/pom.xml 中已有的 `nacos-discovery` 与 `openfeign` 依赖（被子服务继承）。

- [ ] **Step 3: 创建 common/pom.xml**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/common/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <!-- 公共模块聚合工程：以后新增 common-xxx 模块都加在这里 -->
    <parent>
        <groupId>com.wangliang</groupId>
        <artifactId>cloud-demo</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <packaging>pom</packaging>
    <modules>
        <module>common-core</module>
    </modules>

    <artifactId>common</artifactId>
    <name>common</name>
    <description>公共模块聚合工程</description>
</project>
```

- [ ] **Step 4: 创建 common-core/pom.xml**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/common/common-core/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.wangliang</groupId>
        <artifactId>common</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>common-core</artifactId>
    <name>common-core</name>
    <description>公共核心模块：统一返回、错误码、全局异常处理</description>

    <dependencies>
        <!-- web 提供 @RestControllerAdvice 注解与 JSON 序列化支持 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <!-- 单元测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: 删除旧 service-order 目录**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo
git rm -r services/service-order
```

- [ ] **Step 6: 验证聚合结构**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo && mvn -q validate
```

Expected: `BUILD SUCCESS`（无错误输出）

- [ ] **Step 7: 提交**

```bash
git add pom.xml services/pom.xml common/ services/service-order
git commit -m "refactor: 重构聚合父工程（新增 common 模块，service-order 换为 service-stock）"
```

---

### Task 2: common-core 公共模块（R、ResultCode、全局异常）

**Files:**
- Create: `common/common-core/src/main/java/com/wangliang/cloud/common/core/api/R.java`
- Create: `common/common-core/src/main/java/com/wangliang/cloud/common/core/api/ResultCode.java`
- Create: `common/common-core/src/main/java/com/wangliang/cloud/common/core/exception/BusinessException.java`
- Create: `common/common-core/src/main/java/com/wangliang/cloud/common/core/exception/GlobalExceptionHandler.java`
- Test: `common/common-core/src/test/java/com/wangliang/cloud/common/core/api/RTest.java`
- Test: `common/common-core/src/test/java/com/wangliang/cloud/common/core/api/ResultCodeTest.java`

**Interfaces:**
- Consumes: Task 1 的 common-core 模块骨架
- Produces: `R<T>`（静态方法 `ok()/ok(T)/fail(ResultCode)/fail(Integer,String)`）、`ResultCode`（常量 SUCCESS(0)、SYSTEM_ERROR(70000)、PARAM_ERROR(70001)、PRODUCT_NOT_FOUND(70100)、PRODUCT_SAVE_FAIL(70101)、STOCK_NOT_FOUND(70200)、STOCK_NOT_ENOUGH(70201)）、`BusinessException(ResultCode)`、`GlobalExceptionHandler`。Task 3/4/5 全部依赖这些签名

- [ ] **Step 1: 编写失败测试**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/common/common-core/src/test/java/com/wangliang/cloud/common/core/api/RTest.java`：

```java
package com.wangliang.cloud.common.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R 统一返回体测试
 */
class RTest {

    @Test
    void ok_成功时code为0且带数据() {
        R<String> r = R.ok("你好");
        assertEquals(0, r.getCode());
        assertEquals("成功", r.getMsg());
        assertEquals("你好", r.getData());
    }

    @Test
    void fail_按错误码返回() {
        R<Void> r = R.fail(ResultCode.PRODUCT_NOT_FOUND);
        assertEquals(70100, r.getCode());
        assertEquals("商品不存在", r.getMsg());
    }
}
```

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/common/common-core/src/test/java/com/wangliang/cloud/common/core/api/ResultCodeTest.java`：

```java
package com.wangliang.cloud.common.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ResultCode 错误码枚举测试
 */
class ResultCodeTest {

    @Test
    void success_成功码固定为0() {
        assertEquals(0, ResultCode.SUCCESS.getCode());
    }

    @Test
    void 商品不存在_错误码为70100() {
        assertEquals(70100, ResultCode.PRODUCT_NOT_FOUND.getCode());
        assertEquals("商品不存在", ResultCode.PRODUCT_NOT_FOUND.getMsg());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo && mvn -pl common/common-core test
```

Expected: 编译失败（`R`、`ResultCode` 不存在）

- [ ] **Step 3: 创建 R.java**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/common/common-core/src/main/java/com/wangliang/cloud/common/core/api/R.java`：

```java
package com.wangliang.cloud.common.core.api;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回体：项目中所有接口一律返回这个格式。
 * code = 0 表示成功；code != 0 表示失败（具体含义查 ResultCode）。
 */
@Data
public class R<T> implements Serializable {

    /** 状态码：0=成功，非0=失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T data;

    /** 成功（无数据） */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /** 成功（带数据） */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(ResultCode.SUCCESS.getMsg());
        r.setData(data);
        return r;
    }

    /** 失败（按错误码枚举） */
    public static <T> R<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMsg());
    }

    /** 失败（自定义 code + msg，用于 Feign 错误透传） */
    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
```

- [ ] **Step 4: 创建 ResultCode.java**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/common/common-core/src/main/java/com/wangliang/cloud/common/core/api/ResultCode.java`：

```java
package com.wangliang.cloud.common.core.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举：所有错误码集中在这里定义，禁止在代码中写魔法数字。
 * 编号规范：0 = 成功；700xx = 系统/通用；701xx = 商品服务；702xx = 库存服务。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 成功 */
    SUCCESS(0, "成功"),

    /** 系统/通用错误 */
    SYSTEM_ERROR(70000, "系统繁忙，请稍后重试"),
    PARAM_ERROR(70001, "参数错误"),

    /** 商品服务错误（701xx） */
    PRODUCT_NOT_FOUND(70100, "商品不存在"),
    PRODUCT_SAVE_FAIL(70101, "商品保存失败"),

    /** 库存服务错误（702xx） */
    STOCK_NOT_FOUND(70200, "库存记录不存在"),
    STOCK_NOT_ENOUGH(70201, "库存不足");

    /** 错误码 */
    private final Integer code;

    /** 提示信息 */
    private final String msg;
}
```

- [ ] **Step 5: 创建 BusinessException.java**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/common/common-core/src/main/java/com/wangliang/cloud/common/core/exception/BusinessException.java`：

```java
package com.wangliang.cloud.common.core.exception;

import com.wangliang.cloud.common.core.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常：业务规则不满足时抛出（如"商品不存在"），
 * 由 GlobalExceptionHandler 统一转成 R 返回给前端。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 对应的错误码 */
    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }
}
```

- [ ] **Step 6: 创建 GlobalExceptionHandler.java**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/common/common-core/src/main/java/com/wangliang/cloud/common/core/exception/GlobalExceptionHandler.java`：

```java
package com.wangliang.cloud.common.core.exception;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：所有 Controller 抛出的异常统一在这里兜底转成 R，
 * 这样业务代码里不用到处 try-catch，代码更干净。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按错误码原样返回 */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getResultCode());
    }

    /** 参数校验异常：取第一个校验失败的提示 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ResultCode.PARAM_ERROR.getMsg();
        return R.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 兜底异常：未知错误统一返回系统错误码（不打爆细节给前端） */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(ResultCode.SYSTEM_ERROR);
    }
}
```

- [ ] **Step 7: 运行测试验证通过**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo && mvn -pl common/common-core test
```

Expected: `BUILD SUCCESS`，`Tests run: 4, Failures: 0`

- [ ] **Step 8: 提交**

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo
git add common/common-core
git commit -m "feat(common): 新增统一返回R、错误码枚举、全局异常处理"
```

---

### Task 3: service-product 商品服务（骨架 + MySQL）

**Files:**
- Create: `services/service-product/pom.xml`（覆盖为空壳配置）
- Create: `services/service-product/src/main/java/com/wangliang/cloud/product/ProductApplication.java`
- Create: `services/service-product/src/main/resources/application.yml`
- Create: `services/service-product/src/main/resources/db/schema.sql`
- Create: `services/service-product/src/main/java/com/wangliang/cloud/product/entity/Product.java`
- Create: `services/service-product/src/main/java/com/wangliang/cloud/product/mapper/ProductMapper.java`
- Create: `services/service-product/src/main/java/com/wangliang/cloud/product/service/ProductService.java`
- Create: `services/service-product/src/main/java/com/wangliang/cloud/product/service/impl/ProductServiceImpl.java`
- Create: `services/service-product/src/main/java/com/wangliang/cloud/product/controller/ProductController.java`

**Interfaces:**
- Consumes: `R/ResultCode/BusinessException`（Task 2）、`common-core` 依赖、父工程 `mybatis-plus.version`
- Produces: REST 接口 `GET /api/product/list`、`GET /api/product/{id}`、`POST /api/product`、`DELETE /api/product/{id}`；服务名 `service-product` 注册到 Nacos；Task 5 会复用 `GET /api/product/{id}`

- [ ] **Step 1: 重写 service-product/pom.xml**

覆盖 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/pom.xml` 为：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.wangliang</groupId>
        <artifactId>services</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>service-product</artifactId>
    <name>service-product</name>
    <description>商品服务：商品增删改查，Feign 调用库存服务</description>

    <dependencies>
        <!-- 公共模块：R、错误码、全局异常 -->
        <dependency>
            <groupId>com.wangliang</groupId>
            <artifactId>common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!-- Nacos 服务注册发现 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <!-- Feign 远程调用 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <!-- MyBatis-Plus（Spring Boot 3 专用 starter） -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <!-- MySQL 驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建启动类**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/java/com/wangliang/cloud/product/ProductApplication.java`：

```java
package com.wangliang.cloud.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 商品服务启动类。
 * scanBasePackages 必须包含 com.wangliang.cloud，才能扫到公共模块里的全局异常处理器。
 */
@SpringBootApplication(scanBasePackages = "com.wangliang.cloud")
@EnableDiscoveryClient   // 开启服务注册：启动时自动向 Nacos 注册自己
@EnableFeignClients      // 开启 Feign：允许调用其他服务
@MapperScan("com.wangliang.cloud.product.mapper")  // 扫描 MyBatis 的 Mapper 接口
public class ProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/resources/application.yml`：

```yaml
server:
  port: 8081                     # 商品服务端口

spring:
  application:
    name: service-product        # 服务名：在 Nacos 注册中心里的"身份证"
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848   # Nacos 服务端地址（程序连 8848，浏览器才用 18080）
        username: nacos
        password: nacos
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/product_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
  sql:
    init:
      mode: always                       # 启动时自动执行 schema.sql
      schema-locations: classpath:db/schema.sql

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true   # 数据库下划线字段自动映射为驼峰属性
  global-config:
    db-config:
      id-type: assign_id                 # 主键用雪花算法（分布式环境不重复）
```

- [ ] **Step 4: 创建建表脚本**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/resources/db/schema.sql`：

```sql
-- 商品表：商品服务专用数据库 product_db
CREATE TABLE IF NOT EXISTS product (
    id          BIGINT PRIMARY KEY COMMENT '商品ID（雪花算法生成）',
    name        VARCHAR(100) NOT NULL COMMENT '商品名称',
    price       DECIMAL(10,2) NOT NULL COMMENT '价格',
    description VARCHAR(500) COMMENT '描述',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT = '商品表';
```

- [ ] **Step 5: 创建实体 Product**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/java/com/wangliang/cloud/product/entity/Product.java`：

```java
package com.wangliang.cloud.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体：一张表对应一个实体类，字段和表列一一对应。
 */
@Data
@TableName("product")
public class Product {

    /** 商品ID：ASSIGN_ID 表示由雪花算法自动生成 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 价格 */
    private BigDecimal price;

    /** 描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
```

- [ ] **Step 6: 创建 Mapper**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/java/com/wangliang/cloud/product/mapper/ProductMapper.java`：

```java
package com.wangliang.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wangliang.cloud.product.entity.Product;

/**
 * 商品 Mapper：继承 BaseMapper 后自动拥有增删改查方法，
 * 无需手写 SQL（这是 MyBatis-Plus 的核心便利）。
 */
public interface ProductMapper extends BaseMapper<Product> {
}
```

- [ ] **Step 7: 创建 Service 接口与实现**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/java/com/wangliang/cloud/product/service/ProductService.java`：

```java
package com.wangliang.cloud.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wangliang.cloud.product.entity.Product;

/**
 * 商品服务接口：继承 IService 获得通用 CRUD 能力
 */
public interface ProductService extends IService<Product> {
}
```

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/java/com/wangliang/cloud/product/service/impl/ProductServiceImpl.java`：

```java
package com.wangliang.cloud.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wangliang.cloud.product.entity.Product;
import com.wangliang.cloud.product.mapper.ProductMapper;
import com.wangliang.cloud.product.service.ProductService;
import org.springframework.stereotype.Service;

/**
 * 商品服务实现
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}
```

- [ ] **Step 8: 创建 Controller**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/java/com/wangliang/cloud/product/controller/ProductController.java`：

```java
package com.wangliang.cloud.product.controller;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.product.entity.Product;
import com.wangliang.cloud.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品接口：所有返回都是统一格式 R
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 查询商品列表 */
    @GetMapping("/list")
    public R<List<Product>> list() {
        return R.ok(productService.list());
    }

    /** 按 ID 查询商品（Feign 调用库存时会用到） */
    @GetMapping("/{id}")
    public R<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return R.ok(product);
    }

    /** 新增商品 */
    @PostMapping
    public R<Void> save(@RequestBody Product product) {
        product.setCreatedAt(LocalDateTime.now());
        productService.save(product);
        return R.ok();
    }

    /** 删除商品 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productService.removeById(id);
        return R.ok();
    }
}
```

- [ ] **Step 9: 编译验证**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo && mvn -pl services/service-product -am compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 10: 提交**

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo
git add services/service-product
git commit -m "feat(product): 商品服务骨架 + MyBatis-Plus 接入 product_db + Nacos 注册"
```

---

### Task 4: service-stock 库存服务（骨架 + MySQL + 原子扣减）

**Files:**
- Create: `services/service-stock/pom.xml`
- Create: `services/service-stock/src/main/java/com/wangliang/cloud/stock/StockApplication.java`
- Create: `services/service-stock/src/main/resources/application.yml`
- Create: `services/service-stock/src/main/resources/db/schema.sql`
- Create: `services/service-stock/src/main/java/com/wangliang/cloud/stock/entity/Stock.java`
- Create: `services/service-stock/src/main/java/com/wangliang/cloud/stock/mapper/StockMapper.java`
- Create: `services/service-stock/src/main/java/com/wangliang/cloud/stock/service/StockService.java`
- Create: `services/service-stock/src/main/java/com/wangliang/cloud/stock/service/impl/StockServiceImpl.java`
- Create: `services/service-stock/src/main/java/com/wangliang/cloud/stock/controller/StockController.java`

**Interfaces:**
- Consumes: `R/ResultCode/BusinessException`（Task 2）
- Produces: REST 接口 `POST /api/stock`（初始化库存）、`GET /api/stock/{productId}`、`POST /api/stock/deduct/{productId}/{count}`；服务名 `service-stock` 注册到 Nacos（Task 5/6 的 Feign 依赖后两个接口）

- [ ] **Step 1: 创建 service-stock/pom.xml**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-stock/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.wangliang</groupId>
        <artifactId>services</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>service-stock</artifactId>
    <name>service-stock</name>
    <description>库存服务：库存查询、原子扣减</description>

    <dependencies>
        <!-- 公共模块 -->
        <dependency>
            <groupId>com.wangliang</groupId>
            <artifactId>common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!-- Nacos 服务注册发现 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <!-- MySQL 驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建启动类**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-stock/src/main/java/com/wangliang/cloud/stock/StockApplication.java`：

```java
package com.wangliang.cloud.stock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 库存服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.wangliang.cloud")
@EnableDiscoveryClient
@MapperScan("com.wangliang.cloud.stock.mapper")
public class StockApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-stock/src/main/resources/application.yml`：

```yaml
server:
  port: 8082                     # 库存服务端口

spring:
  application:
    name: service-stock          # 服务名：商品服务 Feign 就靠这个名字找到我们
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        username: nacos
        password: nacos
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/stock_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: assign_id
```

- [ ] **Step 4: 创建建表脚本**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-stock/src/main/resources/db/schema.sql`：

```sql
-- 库存表：库存服务专用数据库 stock_db
CREATE TABLE IF NOT EXISTS stock (
    id          BIGINT PRIMARY KEY COMMENT '库存ID（雪花算法生成）',
    product_id  BIGINT NOT NULL UNIQUE COMMENT '商品ID（一个商品一条库存记录）',
    quantity    INT NOT NULL COMMENT '库存数量',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT = '库存表';
```

- [ ] **Step 5: 创建实体 Stock**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-stock/src/main/java/com/wangliang/cloud/stock/entity/Stock.java`：

```java
package com.wangliang.cloud.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存实体
 */
@Data
@TableName("stock")
public class Stock {

    /** 库存ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商品ID：一个商品对应一条库存记录 */
    private Long productId;

    /** 库存数量 */
    private Integer quantity;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 6: 创建 Mapper（含原子扣减 SQL）**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-stock/src/main/java/com/wangliang/cloud/stock/mapper/StockMapper.java`：

```java
package com.wangliang.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wangliang.cloud.stock.entity.Stock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存 Mapper：deductStock 是手写 SQL，保证并发下不超卖。
 */
public interface StockMapper extends BaseMapper<Stock> {

    /**
     * 原子扣减库存：WHERE 里加 quantity >= count 条件。
     * 数据库行锁保证同一时间只有一个请求能扣成功，
     * 库存不够时返回 0，不会扣成负数（防超卖）。
     */
    @Update("UPDATE stock SET quantity = quantity - #{count}, updated_at = NOW() " +
            "WHERE product_id = #{productId} AND quantity >= #{count}")
    int deductStock(@Param("productId") Long productId, @Param("count") Integer count);
}
```

- [ ] **Step 7: 创建 Service 接口与实现**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-stock/src/main/java/com/wangliang/cloud/stock/service/StockService.java`：

```java
package com.wangliang.cloud.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wangliang.cloud.stock.entity.Stock;

/**
 * 库存服务接口
 */
public interface StockService extends IService<Stock> {

    /** 按商品 ID 查询库存 */
    Stock getByProductId(Long productId);

    /**
     * 扣减库存
     * @return true=扣减成功；false=库存不足
     */
    boolean deduct(Long productId, Integer count);
}
```

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-stock/src/main/java/com/wangliang/cloud/stock/service/impl/StockServiceImpl.java`：

```java
package com.wangliang.cloud.stock.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wangliang.cloud.stock.entity.Stock;
import com.wangliang.cloud.stock.mapper.StockMapper;
import com.wangliang.cloud.stock.service.StockService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 库存服务实现
 */
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    @Override
    public Stock getByProductId(Long productId) {
        return lambdaQuery().eq(Stock::getProductId, productId).one();
    }

    @Override
    public boolean deduct(Long productId, Integer count) {
        // 原子扣减：由数据库行锁保证并发安全，返回影响行数
        return baseMapper.deductStock(productId, count) > 0;
    }
}
```

- [ ] **Step 8: 创建 Controller**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-stock/src/main/java/com/wangliang/cloud/stock/controller/StockController.java`：

```java
package com.wangliang.cloud.stock.controller;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.stock.entity.Stock;
import com.wangliang.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存接口：商品服务通过 Feign 调这里的接口
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /** 新增/初始化库存（给商品配库存，测试用） */
    @PostMapping
    public R<Void> save(@RequestBody Stock stock) {
        stock.setUpdatedAt(LocalDateTime.now());
        stockService.save(stock);
        return R.ok();
    }

    /** 按商品 ID 查询库存（Feign 调用入口） */
    @GetMapping("/{productId}")
    public R<Stock> getByProductId(@PathVariable Long productId) {
        Stock stock = stockService.getByProductId(productId);
        if (stock == null) {
            throw new BusinessException(ResultCode.STOCK_NOT_FOUND);
        }
        return R.ok(stock);
    }

    /** 扣减库存（Feign 调用入口） */
    @PostMapping("/deduct/{productId}/{count}")
    public R<Void> deduct(@PathVariable Long productId, @PathVariable Integer count) {
        if (!stockService.deduct(productId, count)) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
        return R.ok();
    }
}
```

- [ ] **Step 9: 编译验证**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo && mvn -pl services/service-stock -am compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 10: 提交**

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo
git add services/service-stock
git commit -m "feat(stock): 库存服务骨架 + 原子扣减 + Nacos 注册"
```

---

### Task 5: 端到端验证（双服务启动 + Nacos 注册 + 数据入库）

**Files:** 无新文件

**Interfaces:**
- Consumes: Task 3、Task 4 的所有服务
- Produces: 验证报告（两个服务在 Nacos 注册成功、数据库可读写）

- [ ] **Step 1: 确保基础设施运行**

运行：

```bash
docker start nacos mysql
```

Expected: 输出两个容器名，无报错

- [ ] **Step 2: 安装 common-core 到本地仓库（服务依赖它）**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo && mvn -q install -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: 后台启动商品服务**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo
mvn -pl services/service-product spring-boot:run > /tmp/service-product.log 2>&1 &
```

Expected: 无报错，进程在后台运行

- [ ] **Step 4: 后台启动库存服务**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo
mvn -pl services/service-stock spring-boot:run > /tmp/service-stock.log 2>&1 &
```

Expected: 无报错，进程在后台运行

- [ ] **Step 5: 等待启动完成**

运行：

```bash
sleep 40 && tail -5 /tmp/service-product.log && echo "=====" && tail -5 /tmp/service-stock.log
```

Expected: 两个日志都出现 `Started ...Application in X seconds` 且无 `ERROR`

- [ ] **Step 6: 验证 Nacos 注册**

运行：

```bash
curl -s "http://127.0.0.1:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=10" | head -c 500
```

Expected: 服务列表包含 `service-product` 和 `service-stock`

- [ ] **Step 7: 验证商品服务读写**

运行：

```bash
# 新增商品
curl -s -X POST http://localhost:8081/api/product \
  -H "Content-Type: application/json" \
  -d '{"name":"《微服务入门》","price":59.90,"description":"适合新手的微服务教程"}'
# 查询列表
curl -s http://localhost:8081/api/product/list
```

Expected: 新增返回 `{"code":0,...}`；列表返回包含刚才的商品

- [ ] **Step 8: 验证库存服务扣减**

运行：

```bash
# 先给商品配一条库存（product_id 用第 7 步返回的 id）
curl -s -X POST http://localhost:8082/api/stock -H "Content-Type: application/json" -d '{"productId":1,"quantity":10}'
# 扣减 3 件
curl -s -X POST http://localhost:8082/api/stock/deduct/1/3
# 查库存
curl -s http://localhost:8082/api/stock/1
```

Expected: 扣减成功返回 `{"code":0}`；查库存 quantity 应为 7

- [ ] **Step 9: 停止两个服务**

运行：

```bash
pkill -f "service-product" ; pkill -f "service-stock" ; sleep 2
```

Expected: 无输出（服务已停止）

---

### Task 6: Feign 远程调用（商品详情带库存）

**Files:**
- Create: `services/service-product/src/main/java/com/wangliang/cloud/product/dto/StockInfoDTO.java`
- Create: `services/service-product/src/main/java/com/wangliang/cloud/product/feign/StockFeignClient.java`
- Modify: `services/service-product/src/main/java/com/wangliang/cloud/product/controller/ProductController.java`（新增 detail 接口）

**Interfaces:**
- Consumes: Task 4 的 `GET /api/stock/{productId}`、`POST /api/stock/deduct/{productId}/{count}`；Task 3 的 `GET /api/product/{id}`
- Produces: 接口 `GET /api/product/{id}/detail`（返回商品+库存）、`POST /api/product/{id}/buy`（下单扣库存，完整演示微服务通信）

- [ ] **Step 1: 创建库存 DTO（跨服务用 DTO，不共享实体，是教科书做法）**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/java/com/wangliang/cloud/product/dto/StockInfoDTO.java`：

```java
package com.wangliang.cloud.product.dto;

import lombok.Data;

/**
 * 库存信息 DTO：商品服务通过 Feign 拿到库存服务的数据就装在这个对象里。
 * 跨服务传输用 DTO 而不是对方的实体类，这样两个服务互不依赖对方的内部结构。
 */
@Data
public class StockInfoDTO {

    /** 库存ID */
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 库存数量 */
    private Integer quantity;
}
```

- [ ] **Step 2: 创建 Feign 客户端**

创建 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/java/com/wangliang/cloud/product/feign/StockFeignClient.java`：

```java
package com.wangliang.cloud.product.feign;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.product.dto.StockInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 库存服务 Feign 客户端。
 * name 必须是库存服务在 Nacos 里注册的服务名，
 * 调用时 Feign 会去 Nacos 找到 service-stock 的真实地址，帮你发 HTTP 请求。
 */
@FeignClient(name = "service-stock")
public interface StockFeignClient {

    /** 查库存：对应库存服务的 GET /api/stock/{productId} */
    @GetMapping("/api/stock/{productId}")
    R<StockInfoDTO> getStock(@PathVariable("productId") Long productId);

    /** 扣库存：对应库存服务的 POST /api/stock/deduct/{productId}/{count} */
    @PostMapping("/api/stock/deduct/{productId}/{count}")
    R<Void> deductStock(@PathVariable("productId") Long productId, @PathVariable("count") Integer count);
}
```

- [ ] **Step 3: 修改商品 Controller 增加 detail 与 buy 接口**

在 `/Users/wangliang/Documents/JavaProject/cloud-demo/services/service-product/src/main/java/com/wangliang/cloud/product/controller/ProductController.java` 中，把类改成（新增字段与两个接口）：

```java
package com.wangliang.cloud.product.controller;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.product.dto.StockInfoDTO;
import com.wangliang.cloud.product.entity.Product;
import com.wangliang.cloud.product.feign.StockFeignClient;
import com.wangliang.cloud.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品接口：所有返回都是统一格式 R
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** Feign 客户端：注入后就能远程调用库存服务 */
    private final StockFeignClient stockFeignClient;

    /** 查询商品列表 */
    @GetMapping("/list")
    public R<List<Product>> list() {
        return R.ok(productService.list());
    }

    /** 按 ID 查询商品（Feign 调用库存时会用到） */
    @GetMapping("/{id}")
    public R<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return R.ok(product);
    }

    /** 新增商品 */
    @PostMapping
    public R<Void> save(@RequestBody Product product) {
        product.setCreatedAt(LocalDateTime.now());
        productService.save(product);
        return R.ok();
    }

    /** 删除商品 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productService.removeById(id);
        return R.ok();
    }

    /**
     * 商品详情（含库存）：演示 Feign 远程调用。
     * 前端拿到商品后，服务端通过 Feign 去库存服务查库存，再合并返回。
     */
    @GetMapping("/{id}/detail")
    public R<Map<String, Object>> detailWithStock(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        // Feign 远程调用库存服务
        R<StockInfoDTO> stockR = stockFeignClient.getStock(id);
        // 若库存服务返回失败，把原始错误透传给前端（不吞错误）
        if (stockR.getCode() != 0) {
            return R.fail(stockR.getCode(), stockR.getMsg());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("stock", stockR.getData());
        return R.ok(result);
    }

    /**
     * 购买商品：查库存 -> 扣库存，完整走一遍微服务通信。
     */
    @PostMapping("/{id}/buy")
    public R<Void> buy(@PathVariable Long id) {
        // 1. 先确认商品存在
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        // 2. Feign 调用库存服务扣减 1 件
        R<Void> stockR = stockFeignClient.deductStock(id, 1);
        if (stockR.getCode() != 0) {
            return R.fail(stockR.getCode(), stockR.getMsg());
        }
        return R.ok();
    }
}
```

- [ ] **Step 4: 编译验证**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo && mvn -pl services/service-product -am compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: 端到端验证（完整业务主线）**

运行：

```bash
cd /Users/wangliang/Documents/JavaProject/cloud-demo && mvn -q install -DskipTests
# 启动两个服务
mvn -pl services/service-product spring-boot:run > /tmp/service-product.log 2>&1 &
mvn -pl services/service-stock spring-boot:run > /tmp/service-stock.log 2>&1 &
sleep 45
# ① 新增商品
curl -s -X POST http://localhost:8081/api/product -H "Content-Type: application/json" \
  -d '{"name":"《微服务从入门到实战》","price":79.00,"description":"完整电商微服务教程"}' && echo ""
# ② 给该商品配库存（product_id 按需改）
curl -s -X POST http://localhost:8082/api/stock -H "Content-Type: application/json" -d '{"productId":1,"quantity":10}' && echo ""
# ③ 商品详情带库存（Feign 调用！）
curl -s http://localhost:8081/api/product/1/detail && echo ""
# ④ 购买（Feign 扣库存）
curl -s -X POST http://localhost:8081/api/product/1/buy && echo ""
# ⑤ 再查库存，应该少 1 件
curl -s http://localhost:8082/api/stock/1
```

Expected: ① `{"code":0}` ② `{"code":0}` ③ 返回 `product` 与 `stock` 两个对象 ④ `{"code":0}` ⑤ quantity 从 10 变 9

- [ ] **Step 6: 停止服务并提交**

运行：

```bash
pkill -f "service-product" ; pkill -f "service-stock" ; sleep 2
cd /Users/wangliang/Documents/JavaProject/cloud-demo
git add services/service-product
git commit -m "feat(product): 新增 Feign 远程调用（商品详情带库存、购买扣库存）"
```

---

### Task 7: 课程笔记（microservice-course 目录）

**Files:**
- Create: `microservice-course/README.md`（在 `/Users/wangliang/Documents/JavaProject/microservice-course/`）
- Create: `microservice-course/00-环境准备/01-Docker部署Nacos与MySQL.md`
- Create: `microservice-course/01-项目骨架/02-父工程与模块划分.md`
- Create: `microservice-course/01-项目骨架/03-公共模块common-core.md`
- Create: `microservice-course/02-服务注册与发现/04-Nacos服务注册与MySQL数据访问.md`
- Create: `microservice-course/03-服务间通信/05-OpenFeign远程调用.md`

**Interfaces:**
- Consumes: Task 1-6 的代码与验证结果
- Produces: 6 篇学习笔记（含思考、坑、关键代码讲解），与 README 的学习进度勾选对齐

- [ ] **Step 1: 创建课程 README**

创建 `/Users/wangliang/Documents/JavaProject/microservice-course/README.md`：

```markdown
# 🎓 微服务学习课程

配套项目：[cloud-demo](../cloud-demo/)

## 📚 学习路线（12 章）

| 章节 | 主题 | 状态 |
|------|------|------|
| 第 01 章 | 环境准备：Docker + Nacos + MySQL | ✅ |
| 第 02-03 章 | 项目骨架：父工程 + 公共模块 | ✅ |
| 第 04-05 章 | 服务注册与配置中心 | 🔄 |
| 第 06-07 章 | 服务间通信：OpenFeign | ✅ |
| 第 08-09 章 | 数据访问：MyBatis-Plus | ✅ |
| 第 10-11 章 | 网关与治理：Gateway + Sentinel | ⏳ |
| 第 12 章+ | 进阶：链路追踪、Seata | ⏳ |
| 🎓 | 毕业项目：商品下单全流程 | ⏳ |

## 🧭 怎么用这份课程

1. 每章一篇笔记，先读"你要学什么"，再跟着做
2. 每个关键概念都有"生活类比"，看完在代码里找到对应位置
3. 每章末尾有"思考题"，做完才算学完
4. 遇到报错，先看"踩坑记录"，没有就自己排查并补充进去
```

- [ ] **Step 2: 创建第 01 章笔记**

创建 `/Users/wangliang/Documents/JavaProject/microservice-course/00-环境准备/01-Docker部署Nacos与MySQL.md`：

```markdown
# 第 01 章：Docker 部署 Nacos 与 MySQL

## 你要学什么
- Docker 的基本概念：镜像（Image）、容器（Container）、端口映射
- 用 Docker 运行 Nacos（注册中心/配置中心）和 MySQL

## 生活类比
- **镜像** = 制作蛋糕的配方；**容器** = 按配方烤出来的蛋糕。一个配方可以烤很多蛋糕。
- **端口映射** = 小区门口的"访客登记"：容器内部的服务，只有映射了端口，外面的浏览器/程序才能访问。

## 核心命令
| 命令 | 作用 |
|------|------|
| `docker ps` | 查看运行中的容器 |
| `docker ps -a` | 查看所有容器（含停止的） |
| `docker start 容器名` | 启动容器 |
| `docker restart 容器名` | 重启容器 |
| `docker logs 容器名` | 看容器日志（排查问题第一招） |
| `docker port 容器名` | 看端口映射 |

## Nacos 访问地址
| 用途 | 地址 | 账号 |
|------|------|------|
| 控制台（浏览器） | http://localhost:18080/ | nacos / nacos |
| 服务端（程序） | http://localhost:8848/nacos/ | - |

## 踩坑记录
1. **Nacos 新版要求认证密钥**：报 `NACOS_AUTH_TOKEN must be set`，加环境变量 `NACOS_AUTH_TOKEN` 即可。
2. **控制台在 8080 端口**：新版 Nacos 控制台从 8848 拆到 8080，端口映射时要加上。
3. **8848 打开是"提示页"**：不是坏了，那是给浏览器看的指路牌；程序用 8848 的服务 API 是正常的。

## 思考题
1. 为什么容器要 `-p 宿主机端口:容器端口` 两个端口？
2. 如果 18080 也被占用，怎么换？
```

- [ ] **Step 3: 创建第 02 章笔记**

创建 `/Users/wangliang/Documents/JavaProject/microservice-course/01-项目骨架/02-父工程与模块划分.md`：

```markdown
# 第 02 章：父工程与模块划分

## 你要学什么
- Maven 多模块项目结构：父工程为什么存在
- 教科书式微服务项目的目录长什么样

## 生活类比
父工程 = 班级的"班主任"：不亲自讲课（不写业务代码），但管着全班同学
（子模块）的作业本规格（依赖版本）。班主任换了（改版本），全班跟着统一变。

## 目录结构
cloud-demo/          # 父工程：管版本，不写业务
├── common/          # 公共模块（全班共用的"课本"）
│   └── common-core/ # 统一返回 R、错误码、异常处理
├── services/        # 业务服务（每个同学各管一科）
│   ├── service-product/  # 商品服务（8081）
│   └── service-stock/    # 库存服务（8082）
└── docs/            # 设计文档、实施计划

## 关键概念
- `<dependencyManagement>`：只声明版本，不真正引入依赖；子模块声明依赖时不用写版本号
- 父 pom 的 `<packaging>pom</packaging>`：聚合工程标记
- `-am` 参数：编译时连依赖的模块一起编译（如 `mvn -pl services/service-product -am compile`）

## 思考题
1. 为什么每个服务要独立的数据库？（提示：独立部署、独立故障）
2. 为什么父工程里版本号集中管理？
```

- [ ] **Step 4: 创建第 03 章笔记**

创建 `/Users/wangliang/Documents/JavaProject/microservice-course/01-项目骨架/03-公共模块common-core.md`：

```markdown
# 第 03 章：公共模块 common-core

## 你要学什么
- 统一返回体 R 为什么重要
- 错误码枚举的正确用法
- 全局异常处理器的原理

## 生活类比
R = 全班统一的"作业本格式"：不管哪个科目（服务），交上来的格式都一样，
批改（前端解析）就很简单。

## 三个核心类
### R<T>：统一返回体
所有接口返回 `{"code":0,"msg":"成功","data":...}`。code=0 成功，非 0 失败。

### ResultCode：错误码枚举
错误码分段：0 成功 / 700xx 系统 / 701xx 商品 / 702xx 库存。
**规则：禁止在代码里写 `R.fail("商品不存在")` 这种裸提示，必须用枚举。**

### GlobalExceptionHandler：全局异常处理器
业务代码里 `throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND)`，
处理器统一接住转成 R 返回。**业务代码不再到处 try-catch。**

## 踩坑记录
**启动类必须 `scanBasePackages = "com.wangliang.cloud"`**：否则扫不到公共模块里的
全局异常处理器，异常会变成 Spring 默认错误页。

## 思考题
1. 为什么 Feign 调用失败时要"透传"原始 code 和 msg？
2. 错误码为什么要分段？（提示：以后排查日志只看编号就知道是哪个服务）
```

- [ ] **Step 5: 创建第 04 章笔记**

创建 `/Users/wangliang/Documents/JavaProject/microservice-course/02-服务注册与发现/04-Nacos服务注册与MySQL数据访问.md`：

```markdown
# 第 04 章：Nacos 服务注册与 MySQL 数据访问

## 你要学什么
- 服务怎么"报到"进 Nacos（服务注册）
- MyBatis-Plus 怎么让 CRUD 零 SQL

## 生活类比
Nacos = 学校的"花名册"：每个新同学（服务）开学报到（注册）登记姓名和住址（IP），
别人要找人（调用服务）就翻花名册，不用记每个人的电话号码。

## 服务注册三步
1. pom 加依赖 `spring-cloud-starter-alibaba-nacos-discovery`
2. yml 配 `spring.application.name`（花名册上的名字）+ `nacos.server-addr`
3. 启动类加 `@EnableDiscoveryClient`

## MyBatis-Plus 零 SQL 的秘密
Mapper 继承 `BaseMapper<Product>` 就自动有增删改查；
Service 继承 `IService<Product>` 自动有 `save/list/getById`。
**一张表 = 一个实体 + 一个 Mapper + 一个 Service，三件套。**

## 防超卖：原子扣减
```sql
UPDATE stock SET quantity = quantity - #{count}
WHERE product_id = #{productId} AND quantity >= #{count}
```
WHERE 里的 `quantity >= count` + 数据库行锁 = 并发下不会扣成负数。

## 思考题
1. 如果商品服务挂了，Nacos 花名册上会怎样？（提示：心跳机制）
2. 原子扣减为什么比"先查再改"安全？
```

- [ ] **Step 6: 创建第 05 章笔记**

创建 `/Users/wangliang/Documents/JavaProject/microservice-course/03-服务间通信/05-OpenFeign远程调用.md`：

```markdown
# 第 05 章：OpenFeign 远程调用

## 你要学什么
- 服务 A 怎么调用服务 B（跨服务 HTTP 调用）
- Feign 接口怎么写、怎么用

## 生活类比
Feign = 帮你跑腿的"快递员"：你（商品服务）想拿库存服务的东西，不用自己开车
（手写 HTTP 请求），打个电话（声明接口方法）快递员就帮你取回来。

## 三个步骤
1. 启动类加 `@EnableFeignClients`
2. 写接口 `@FeignClient(name = "service-stock")`，方法签名照抄库存服务的接口
3. Controller 里注入接口，直接当本地方法调用

```java
@FeignClient(name = "service-stock")   // name = 对方服务在 Nacos 的花名册名字
public interface StockFeignClient {
    @GetMapping("/api/stock/{productId}")   // 对方服务的真实路径
    R<StockInfoDTO> getStock(@PathVariable Long productId);
}
```

## 关键规则
- **跨服务用 DTO，不用对方的实体类**：两个服务保持独立
- **错误要透传**：库存服务返回失败码，商品服务要原样返回给前端，不能吞

## 思考题
1. Feign 是怎么知道"service-stock"在哪个 IP 的？（提示：Nacos 服务发现）
2. 如果库存服务没启动，调用会怎样？去试试！
```

- [ ] **Step 7: 更新 cloud-demo README 学习进度**

编辑 `/Users/wangliang/Documents/JavaProject/cloud-demo/README.md`：

- 功能列表：勾选 `P1~P4`（把已完成的规划项改为 `[x]` 并在描述后补"✅"）
- 学习进度：勾选第 01 章、02-03 章、06-07 章、08-09 章

- [ ] **Step 8: 提交课程与 README**

运行：

```bash
mkdir -p /Users/wangliang/Documents/JavaProject/microservice-course
cd /Users/wangliang/Documents/JavaProject/microservice-course && git init && git add . && git commit -m "docs: 微服务学习课程 第 01-05 章笔记"
cd /Users/wangliang/Documents/JavaProject/cloud-demo && git add README.md && git commit -m "docs: 更新 README 学习进度（P1-P4 完成）"
```

> 注：microservice-course 独立成库（课程是长期内容，与代码仓库分开），
> 或作为子目录放在 cloud-demo 仓库内——选择其一并保持一致。
> 本计划采用：microservice-course 与 cloud-demo 平级、独立 git 仓库。
