package com.wangliang.cloud.order.dto;

import lombok.Data;

/**
 * 库存信息 DTO：下单扣库存时通过 Feign 从库存服务取回的库存数据。
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
