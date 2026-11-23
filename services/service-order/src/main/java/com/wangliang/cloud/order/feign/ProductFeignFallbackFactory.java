package com.wangliang.cloud.order.feign;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.order.dto.ProductInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 商品服务 Feign 降级工厂：商品服务超时 / 宕机 / 熔断时返回失败 R，下单快速失败。
 * 原理同 {@link StockFeignFallbackFactory}。
 */
@Slf4j
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable cause) {
        String reason = StockFeignFallbackFactory.describe(cause);
        return productId -> {
            log.warn("商品服务查询降级，productId={}, 原因={}", productId, reason, cause);
            return R.<ProductInfoDTO>fail(ResultCode.SERVICE_UNAVAILABLE.getCode(),
                    "商品服务暂不可用（" + reason + "），请稍后重试");
        };
    }
}
