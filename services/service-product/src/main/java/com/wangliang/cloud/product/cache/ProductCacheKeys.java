package com.wangliang.cloud.product.cache;

import com.wangliang.cloud.product.config.ProductCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 商品缓存 Key 生成器。
 *
 * <p>集中生成 Key，避免业务代码到处手写字符串，也方便以后统一修改命名空间。
 * 当前只提供商品信息 Key；列表、热门商品等 Key 等真正需要时再补充。</p>
 */
@Component
@RequiredArgsConstructor
public class ProductCacheKeys {

    private final ProductCacheProperties properties;

    /**
     * 生成商品基础信息缓存 Key，例如 cloud-demo:product:info:1001。
     * <p>
     * 只存商品信息、不含库存。旧版的 :detail: Key 存的是"商品 + 库存快照"，
     * 换命名空间可以让旧格式的数据自然过期，不会被新代码读到。
     */
    public String info(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        return properties.getKeyPrefix() + ":info:" + productId;
    }
}
