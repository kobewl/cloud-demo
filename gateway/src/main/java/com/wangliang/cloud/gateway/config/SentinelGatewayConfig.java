package com.wangliang.cloud.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Sentinel 网关限流配置（P7 遗留 + P8 改造）。
 *
 * 限流为什么做在网关？
 *   网关是所有请求的唯一入口，在网关上限流 = 在"大门"处拦，
 *   不用每个服务各装一套限流，治理成本最低。
 *
 * P8 改造：限流规则从"Java 硬编码"改为"Nacos 数据源动态拉取"。
 *   · 规则不再写在这里，而是放在 Nacos 的 gateway-flow.json（见 docs/nacos-config/）
 *   · 数据源配置见 application.yml 的 spring.cloud.sentinel.datasource
 *   · 改 QPS 只需改 Nacos 配置并发布，网关不重启，秒级生效
 *   · 本类只保留"被限流时的降级返回"（HTTP 429 + JSON）
 */
@Configuration
public class SentinelGatewayConfig {

    /**
     * 被限流时的降级返回：HTTP 429 + 友好 JSON。
     * @PostConstruct：Gateway 应用启动完成后自动执行一次。
     */
    @PostConstruct
    public void init() {
        GatewayCallbackManager.setBlockHandler((exchange, t) ->
                ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\"}"));
    }
}
