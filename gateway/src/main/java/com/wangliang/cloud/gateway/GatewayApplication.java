package com.wangliang.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API 网关启动类。
 * 网关是 WebFlux 应用（响应式，不是传统 WebMVC），负责统一入口 + 路由转发，
 * 不写业务代码。所有请求先进网关，再按路由规则转发到具体微服务。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
