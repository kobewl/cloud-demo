package com.wangliang.cloud.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 商品缓存参数。
 *
 * <p>把 TTL、开关和 Key 前缀集中管理，避免缓存策略散落在 Controller 和 Service 中。
 * 真正的 Cache Aside 读写流程暂时不在这里实现，留给学习阶段练习。</p>
 */
@Data
@ConfigurationProperties(prefix = "cache.product")
public class ProductCacheProperties {

    /** 是否启用商品缓存，便于排查问题时快速旁路 Redis。 */
    private boolean enabled = true;

    /** 商品详情缓存过期时间。 */
    private Duration detailTtl = Duration.ofMinutes(10);

    /** 商品缓存 Key 的命名空间。 */
    private String keyPrefix = "cloud-demo:product";
}
