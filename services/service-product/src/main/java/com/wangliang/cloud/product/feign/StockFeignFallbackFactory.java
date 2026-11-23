package com.wangliang.cloud.product.feign;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.product.dto.StockInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 库存服务 Feign 降级工厂（商品服务侧）。
 * <p>
 * 库存服务超时 / 宕机 / 被 Sentinel 熔断时，不再让商品详情接口卡满 read-timeout，
 * 而是立刻返回"库存服务暂不可用"。熔断打开期间请求根本不发出去，给库存服务留出恢复时间。
 */
@Slf4j
@Component
public class StockFeignFallbackFactory implements FallbackFactory<StockFeignClient> {

    @Override
    public StockFeignClient create(Throwable cause) {
        String reason = describe(cause);
        String msg = "库存服务暂不可用（" + reason + "），请稍后重试";
        return new StockFeignClient() {
            @Override
            public R<StockInfoDTO> getStock(Long productId) {
                log.warn("库存查询降级，productId={}, 原因={}", productId, reason, cause);
                return R.fail(ResultCode.SERVICE_UNAVAILABLE.getCode(), msg);
            }

            @Override
            public R<Void> deductStock(Long productId, Integer count) {
                log.warn("库存扣减降级，productId={}, count={}, 原因={}", productId, count, reason, cause);
                return R.fail(ResultCode.SERVICE_UNAVAILABLE.getCode(), msg);
            }
        };
    }

    /** 熔断打开 → "熔断中"；否则取根因类名（Feign 会把超时/连接异常包在 RetryableException 里） */
    private static String describe(Throwable cause) {
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
