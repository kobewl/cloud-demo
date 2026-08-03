package com.wangliang.cloud.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体：一张表对应一个实体类，字段和表列一一对应。
 */
@Data
@TableName("product")
public class Product {

    /** 商品ID：ASSIGN_ID 表示由雪花算法自动生成 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 价格 */
    private BigDecimal price;

    /** 描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
