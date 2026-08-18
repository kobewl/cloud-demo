package com.wangliang.cloud.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wangliang.cloud.order.dto.OrderCreateDTO;
import com.wangliang.cloud.order.entity.Order;

/**
 * 订单服务接口
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单：查商品 -> 扣库存 -> 落订单（含失败补偿）
     *
     * @param dto 下单参数（商品ID、数量）
     * @return 订单ID
     */
    Long create(OrderCreateDTO dto);
}
