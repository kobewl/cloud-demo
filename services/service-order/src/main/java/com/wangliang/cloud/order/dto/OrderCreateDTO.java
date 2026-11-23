package com.wangliang.cloud.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 下单请求参数：前端 POST /api/order/create 传这个对象。
 * 校验注解会在 Controller 入口自动生效，失败由全局异常处理器转成 R 返回。
 */
@Data
public class OrderCreateDTO {

    /**
     * 幂等键（面试考点：接口幂等）。
     * 前端在"打开下单表单 / 改了商品或数量"时生成一个 UUID，提交时带上；
     * 用户连点、网络超时后重发，带的都是同一个键 → 后端只落一单，重复请求直接返回第一单的 ID。
     */
    @NotBlank(message = "requestId 不能为空")
    @Size(max = 64, message = "requestId 长度不能超过 64")
    private String requestId;

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 购买数量 */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为 1")
    private Integer count;
}
