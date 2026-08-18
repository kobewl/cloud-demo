package com.wangliang.cloud.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 下单请求参数：前端 POST /api/order/create 传这个对象。
 * 校验注解会在 Controller 入口自动生效，失败由全局异常处理器转成 R 返回。
 */
@Data
public class OrderCreateDTO {

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 购买数量 */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为 1")
    private Integer count;
}
