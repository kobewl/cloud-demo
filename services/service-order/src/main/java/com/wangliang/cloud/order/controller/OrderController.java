package com.wangliang.cloud.order.controller;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.order.dto.OrderCreateDTO;
import com.wangliang.cloud.order.entity.Order;
import com.wangliang.cloud.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单接口：所有返回都是统一格式 R
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单：POST /api/order/create
     * 参数校验失败时（如商品ID为空、数量小于1），由全局异常处理器转成 R 返回。
     */
    @PostMapping("/create")
    public R<Long> create(@RequestBody @Valid OrderCreateDTO dto) {
        Long orderId = orderService.create(dto);
        return R.ok(orderId);
    }

    /**
     * 按 ID 查询订单：GET /api/order/{id}
     */
    @GetMapping("/{id}")
    public R<Order> getById(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return R.ok(order);
    }

    /**
     * 订单列表：GET /api/order/list
     */
    @GetMapping("/list")
    public R<List<Order>> list() {
        List<Order> orders = orderService.list();
        return R.ok(orders);
    }
}
