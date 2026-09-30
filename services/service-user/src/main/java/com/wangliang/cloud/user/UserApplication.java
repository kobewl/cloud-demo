package com.wangliang.cloud.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户服务启动类（P15 JWT A）。
 * scanBasePackages 必须包含 com.wangliang.cloud，才能扫到公共模块里的全局异常处理器。
 */
@SpringBootApplication(scanBasePackages = "com.wangliang.cloud")
@EnableDiscoveryClient   // 开启服务注册：启动时自动向 Nacos 注册自己
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
