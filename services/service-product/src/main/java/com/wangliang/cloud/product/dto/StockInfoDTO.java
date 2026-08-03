package com.wangliang.cloud.product.dto;

import lombok.Data;

/**
 * 库存信息 DTO：商品服务通过 Feign 拿到库存服务的数据就装在这个对象里。
 * 跨服务传输用 DTO 而不是对方的实体类，这样两个服务互不依赖对方的内部结构。
 */
@Data
public class StockInfoDTO {

    /** 库存ID */
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 库存数量 */
    private Integer quantity;
}
