package com.wangliang.cloud.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品信息 DTO：下单时通过 Feign 从商品服务取回的商品数据。
 * 跨服务传输用 DTO 而不是对方的实体类，保持两个服务互相独立。
 */
@Data
public class ProductInfoDTO {

    /** 商品ID */
    private Long id;

    /** 商品名称 */
    private String name;

    /** 价格 */
    private BigDecimal price;
}
