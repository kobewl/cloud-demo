package com.wangliang.cloud.common.core.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 序列化配置：把 Long 类型统一序列化成字符串。
 *
 * <p>为什么需要它：本项目所有实体（商品/库存/订单）的主键 ID 都用 MyBatis-Plus 的
 * 雪花算法（ASSIGN_ID）生成，是 19 位长整型，远超 JavaScript number 的精确范围
 * （2^53 - 1 ≈ 9×10^15）。若原样以数字返回，前端 JSON 解析后会丢失精度，
 * 导致"按 ID 删除/查询"时把 A 商品当成 B 商品。
 *
 * <p>解决办法：后端序列化时把 Long 转成字符串（如 "1780000000000000000"），
 * 前端 ID 字段统一用 string 接收，精度不丢、功能正常。
 *
 * <p>生效范围：业务服务（product/stock/order）的启动类都用了
 * {@code @SpringBootApplication(scanBasePackages = "com.wangliang.cloud")}，
 * 能扫描到本配置类；网关只做路由转发、不序列化业务 JSON，无需此配置。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            // Long 包装类型 -> 字符串（实体里的 id、productId 都是包装类型 Long）
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            // long 基本类型 -> 字符串（兜底，防止未来有人用 long 字段）
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
