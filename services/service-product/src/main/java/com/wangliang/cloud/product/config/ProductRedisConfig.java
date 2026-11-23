package com.wangliang.cloud.product.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 商品服务 Redis 基础配置。
 *
 * <p>直接使用 Spring Boot 自动装配的 {@code StringRedisTemplate}：Key 和 Value 都是字符串，
 * redis-cli 里能直接看懂；Value 的 JSON 由 {@code ProductInfoCache} 用 ObjectMapper 显式按类型读写。</p>
 *
 * <p>之前这里自定义过 {@code RedisTemplate<String, Object>} + {@code GenericJackson2JsonRedisSerializer(objectMapper)}，
 * 踩了一个坑：传入外部 ObjectMapper 的构造器不会开启 default typing，JSON 里没有 @class，
 * 读回来变成 LinkedHashMap，业务代码的 instanceof 判断永远不成立，缓存只写不中。</p>
 */
@Configuration
@EnableConfigurationProperties(ProductCacheProperties.class)
public class ProductRedisConfig {
}
