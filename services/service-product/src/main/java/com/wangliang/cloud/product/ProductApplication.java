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
