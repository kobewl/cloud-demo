package com.wangliang.cloud.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 网关限流配置（P7）。
 *
 * 限流为什么做在网关？
 *   网关是所有请求的唯一入口，在网关上限流 = 在"大门"处拦，
 *   不用每个服务各装一套限流，治理成本最低。
 *
 * Sentinel 对 Spring Cloud Gateway 的限流粒度：
 *   按路由（route）维度。每条 route 是一个独立资源，可以单独设 QPS 阈值。
 *   例如 order-route（下单是贵重操作）限严一点，product-route 放宽一点。
 */
@Configuration
public class SentinelGatewayConfig {

    /**
     * 注册网关限流规则 + 被限流时的降级返回。
     * @PostConstruct：Gateway 应用启动完成后自动执行一次。
     */
    @PostConstruct
    public void init() {
        // 1 定义限流规则：每条路由 = 一个独立资源，各设各的 QPS 上限
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule("order-route")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
                .setCount(2)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule("product-route")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
                .setCount(5)
                .setIntervalSec(1));

        // 2 把规则加载进 sentinel
        GatewayRuleManager.loadRules(rules);

        // 3. 被限流时的降级处理，返回 http 429 + json
        GatewayCallbackManager.setBlockHandler((exchange, t) ->
                ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\"}"));

    }
}
