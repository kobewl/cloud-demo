package com.wangliang.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wangliang.cloud.product.entity.Product;

/**
 * 商品 Mapper：继承 BaseMapper 后自动拥有增删改查方法，
 * 无需手写 SQL（这是 MyBatis-Plus 的核心便利）。
 */
public interface ProductMapper extends BaseMapper<Product> {
}
