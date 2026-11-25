package com.wangliang.cloud.order.service.impl;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.order.feign.StockFeignClient;
import com.wangliang.cloud.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Component;

/**
 * 模拟付款的全局事务本体：先条件把待付款改为已支付，成功后再确认预占。
 * 单独做成 Bean，是因为 {@code @GlobalTransactional} 靠 Spring AOP 代理生效，
 * 同类里自调用会绕过代理（与 {@link OrderCreateTx}、{@link OrderTimeoutTx} 相同）。
 */
@Component
@RequiredArgsConstructor
public class OrderPayTx {

    private final OrderMapper orderMapper;

    private final StockFeignClient stockFeignClient;

    @GlobalTransactional(timeoutMills = 30000, name = "order-pay")
    public void pay(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        if (orderMapper.payIfPending(orderId) == 0){
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        R<Void> confirmR = stockFeignClient.confirmHold(orderId);
        if (confirmR.getCode() != 0){
            throw new BusinessException(confirmR.getCode(),confirmR.getMsg());
        }
    }
}
