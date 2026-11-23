package com.wangliang.cloud.product.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.wangliang.cloud.product.config.ProductCacheProperties;
import com.wangliang.cloud.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductInfoCacheTest {

    private static final String KEY = "cloud-demo:product:info:1";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> ops;
    private ProductCacheProperties properties;
    private ProductInfoCache cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        properties = new ProductCacheProperties();
        // 和线上一致的 ObjectMapper：JavaTimeModule + common-core 的 Long 转字符串
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .serializerByType(Long.class, ToStringSerializer.instance)
                .build();
        cache = new ProductInfoCache(redisTemplate, objectMapper, new ProductCacheKeys(properties), properties);
    }

    /** 回归：旧实现读回来是 LinkedHashMap，缓存永远不命中 */
    @Test
    void putThenGetRoundTripsAsProduct() {
        Product product = product();
        cache.put(product);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(ops).set(eq(KEY), json.capture(), eq(Duration.ofMinutes(10)));
        when(ops.get(KEY)).thenReturn(json.getValue());

        assertThat(cache.get(1L)).get().isEqualTo(product);
    }

    @Test
    void cachedJsonNeverContainsStock() {
        cache.put(product());

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(ops).set(anyString(), json.capture(), any(Duration.class));
        assertThat(json.getValue()).doesNotContain("stock").doesNotContain("quantity");
    }

    @Test
    void redisDownIsTreatedAsMiss() {
        when(ops.get(KEY)).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(cache.get(1L)).isEmpty();
    }

    @Test
    void corruptedEntryIsEvictedAndTreatedAsMiss() {
        when(ops.get(KEY)).thenReturn("{not json");

        assertThat(cache.get(1L)).isEmpty();
        verify(redisTemplate).delete(KEY);
    }

    @Test
    void disabledCacheNeverTouchesRedis() {
        properties.setEnabled(false);

        assertThat(cache.get(1L)).isEmpty();
        cache.put(product());
        cache.evict(1L);
        verifyNoInteractions(redisTemplate);
    }

    private static Product product() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Java 并发编程");
        p.setPrice(new BigDecimal("59.90"));
        p.setDescription("desc");
        p.setCreatedAt(LocalDateTime.of(2026, 11, 23, 10, 0));
        return p;
    }
}
