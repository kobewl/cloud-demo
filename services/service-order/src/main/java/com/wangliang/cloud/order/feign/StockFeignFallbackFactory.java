package com.wangliang.cloud.order.feign;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 库存服务 Feign 降级工厂（面试考点：熔断降级）。
 * <p>
 * 触发时机（由 Sentinel 包装 Feign 调用，需 feign.sentinel.enabled=true）：
 * <ul>
 *   <li>调用抛异常：连接拒绝（库存服务挂了）、读超时（库存服务慢）、HTTP 5xx 等；</li>
 *   <li>熔断器打开：Sentinel 降级规则判定库存服务"不健康"，直接不发请求（BlockException），快速失败。</li>
 * </ul>
 * 用 FallbackFactory 而不是 fallback：能拿到 cause，日志里分得清是"超时"还是"熔断"。
 * <p>
 * ⚠ 下单场景的降级只能是"失败"，不能编造"扣减成功"：返回失败 R → 下单方抛异常 → Seata 全局回滚。
 */
@Slf4j
@Component
public class StockFeignFallbackFactory implements FallbackFactory<StockFeignClient> {

    @Override
    public StockFeignClient create(Throwable cause) {
        String reason = describe(cause);
        return new StockFeignClient() {
            @Override
            public R<Void> deductStock(Long productId, Integer count) {
                log.warn("库存服务扣减降级，productId={}, count={}, 原因={}", productId, count, reason, cause);
                return unavailable(reason);
            }

            @Override
            public R<Void> addStock(Long productId, Integer count) {
                log.warn("库存服务回补降级，productId={}, count={}, 原因={}", productId, count, reason, cause);
                return unavailable(reason);
            }
        };
    }

    private static R<Void> unavailable(String reason) {
        return R.fail(ResultCode.SERVICE_UNAVAILABLE.getCode(), "库存服务暂不可用（" + reason + "），请稍后重试");
    }

    /**
     * 熔断打开 → "熔断中"；否则取根因类名（Feign 会把 SocketTimeoutException / ConnectException
     * 包在 RetryableException 里，直接看外层分不清是超时还是连不上）。
     */
    static String describe(Throwable cause) {
        if (BlockException.isBlockException(cause)) {
            return "熔断中";
        }
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName();
    }
}
