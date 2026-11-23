package com.wangliang.cloud.product.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.cloud.product.config.ProductCacheProperties;
import com.wangliang.cloud.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 商品基础信息缓存（Cache Aside 的读 / 写 / 失效三个动作都收在这里）。
 *
 * <p><b>只缓存商品信息，不缓存库存</b>（面试考点："库存这种高频变化的数据该不该缓存"）：
 * 库存每下一单就变，且扣减发生在 service-stock / service-order，商品服务感知不到，
 * 缓存进来必然出现"下单后详情页库存不变"的脏读。所以动静分离：
 * 名称、价格这类低频变化的数据进缓存；库存每次实时查（秒杀场景再单独用 Redis 预扣库存方案）。</p>
 *
 * <p><b>为什么用 StringRedisTemplate + 显式指定类型反序列化</b>：
 * 之前用 GenericJackson2JsonRedisSerializer(objectMapper) 传入了 Spring 的 ObjectMapper，
 * 这个构造器不会写入 @class 类型信息，读回来是 LinkedHashMap，
 * {@code instanceof} 判断永远为 false —— 缓存只写不中。
 * 读的时候明确告诉 Jackson 目标类型，既不依赖 @class，也避开了"多态反序列化"的安全隐患。</p>
 *
 * <p><b>Redis 故障不影响主流程</b>：所有 Redis 异常只记日志，读失败当未命中（回源 DB），
 * 写/删失败靠 TTL 兜底过期。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductInfoCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductCacheKeys keys;
    private final ProductCacheProperties properties;

    /**
     * 读缓存。
     *
     * @return 命中返回商品；未命中、缓存关闭、Redis 异常、数据损坏都返回 empty（调用方回源 DB）
     */
    public Optional<Product> get(Long productId) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        String key = keys.info(productId);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, Product.class));
        } catch (JsonProcessingException e) {
            // 数据格式不对（比如改了实体结构后的旧数据）：删掉坏数据，回源
            log.warn("商品缓存数据损坏，删除后回源，cacheKey={}", key, e);
            evict(productId);
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("读取商品缓存失败，回源 DB，cacheKey={}", key, e);
            return Optional.empty();
        }
    }

    /** 回填缓存（查 DB 之后调用）。 */
    public void put(Product product) {
        if (!properties.isEnabled() || product == null || product.getId() == null) {
            return;
        }
        String key = keys.info(product.getId());
        try {
            String json = objectMapper.writeValueAsString(product);
            redisTemplate.opsForValue().set(key, json, properties.getDetailTtl());
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("写入商品缓存失败，cacheKey={}", key, e);
        }
    }

    /** 失效缓存（改/删 DB 之后调用：先更新数据库，再删缓存）。 */
    public void evict(Long productId) {
        if (!properties.isEnabled()) {
            return;
        }
        String key = keys.info(productId);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("删除商品缓存失败，等待 TTL 过期，cacheKey={}", key, e);
        }
    }
}
