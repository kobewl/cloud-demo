package com.wangliang.cloud.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务启动类。
 * scanBasePackages 必须包含 com.wangliang.cloud，才能扫到公共模块里的全局异常处理器。
 * @EnableFeignClients 开启 Feign：下单时用它调商品服务、库存服务。
 */
@SpringBootApplication(scanBasePackages = "com.wangliang.cloud")
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.wangliang.cloud.order.mapper")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
