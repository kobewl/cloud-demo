package com.wangliang.cloud.product.cache;

import com.wangliang.cloud.product.config.ProductCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 商品缓存 Key 生成器。
 *
 * <p>集中生成 Key，避免业务代码到处手写字符串，也方便以后统一修改命名空间。
 * 当前只提供详情 Key；列表、热门商品等 Key 等真正需要时再补充。</p>
 */
@Component
@RequiredArgsConstructor
public class ProductCacheKeys {

    private final ProductCacheProperties properties;

    /**
     * 生成商品详情缓存 Key，例如 cloud-demo:product:detail:1001。
     */
    public String detail(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        return properties.getKeyPrefix() + ":detail:" + productId;
    }
}
