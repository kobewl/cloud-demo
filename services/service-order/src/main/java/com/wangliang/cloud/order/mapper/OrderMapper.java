package com.wangliang.cloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wangliang.cloud.order.entity.Order;

/**
 * 订单 Mapper：继承 BaseMapper 后自动拥有增删改查方法，
 * 无需手写 SQL（这是 MyBatis-Plus 的核心便利）。
 * 逻辑删除由 @TableLogic + 全局配置自动处理，无需在 SQL 里手动加 deleted 条件。
 */
public interface OrderMapper extends BaseMapper<Order> {
}
