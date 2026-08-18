package com.wangliang.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体：对应 orders 表。
 * 表名用 orders（order 是 MySQL 保留字，直接用会报语法错误）。
 */
@Data
@TableName("orders")
public class Order {

    /** 订单ID：ASSIGN_ID 表示由雪花算法自动生成 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 商品名称（下单时快照，商品改名不影响历史订单） */
    private String productName;

    /** 成交单价 */
    private BigDecimal price;

    /** 购买数量 */
    private Integer count;

    /** 总金额 = 单价 x 数量 */
    private BigDecimal totalAmount;

    /** 状态：0-已创建 1-已支付 2-已取消 */
    private Integer status;

    /** 逻辑删除标记：0-未删除 1-已删除（加 @TableLogic，配合全局配置） */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间（数据库 ON UPDATE 自动刷新，Java 侧不用手动维护） */
    private LocalDateTime updatedAt;
}
