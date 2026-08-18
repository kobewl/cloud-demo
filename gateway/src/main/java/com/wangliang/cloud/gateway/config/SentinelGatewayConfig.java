package com.wangliang.cloud.gateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

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
     * TODO(王栋)：补全方法体
     *   ① 用 GatewayFlowRule 定义两条限流规则：
     *      - "order-route"：QPS 上限 2，窗口 1 秒（下单限严）
     *      - "product-route"：QPS 上限 5，窗口 1 秒
     *   ② GatewayRuleManager.loadRules(rules) 加载规则
     *   ③ GatewayCallbackManager.setBlockHandler(...) 注册降级处理：
     *      被限流时返回 JSON：{"code":429,"msg":"请求过于频繁，请稍后再试"}
     *      （提示：BlockRequestHandler 是函数式接口，可写 lambda；
     *       返回用 ServerResponse.status(429).contentType(APPLICATION_JSON).bodyValue(...)）
     */
    @PostConstruct
    public void init() {
        // TODO(王栋)：补全
    }
}
