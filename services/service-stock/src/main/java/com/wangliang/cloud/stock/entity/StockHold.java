package com.wangliang.cloud.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存预占记录：一笔订单对应一条预占（一单一个商品）。
 * 对应表 stock_hold。主键为订单ID，由业务在插入时显式传入，不自动生成。
 */
@Data
@TableName("stock_hold")
public class StockHold {

    /** 订单ID（主键）：由下单侧生成后传入，不在本实体内自动生成 */
    @TableId(value = "order_id", type = IdType.INPUT)
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 预占数量 */
    private Integer count;

    /** 状态：0-预占中 1-已确认 2-已释放 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
