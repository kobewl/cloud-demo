package com.wangliang.cloud.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存实体
 */
@Data
@TableName("stock")
public class Stock {

    /** 库存ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商品ID：一个商品对应一条库存记录 */
    private Long productId;

    /** 库存数量 */
    private Integer quantity;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
