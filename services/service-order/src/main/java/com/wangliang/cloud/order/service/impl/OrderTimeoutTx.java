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
 * 超时关单的全局事务本体：先条件关闭到期未付款订单，成功后再释放预占库存。
 * 单独做成 Bean，是因为 {@code @GlobalTransactional} 靠 Spring AOP 代理生效，
 * 同类里自调用会绕过代理（与 {@link OrderCreateTx} 相同）。
 */
@Component
@RequiredArgsConstructor
public class OrderTimeoutTx {

    private final OrderMapper orderMapper;

    private final StockFeignClient stockFeignClient;

    @GlobalTransactional(timeoutMills = 30000, name = "order-timeout-close")
    public void close(Long orderId) {
        // 校验
        if (orderId == null){
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        // 没有成功关掉，就不能释放库存
        if (orderMapper.closeExpired(orderId) == 0){
            return;
        }

        R<Void> releaseR = stockFeignClient.releaseHold(orderId);
        if (releaseR.getCode() != 0){
            throw new BusinessException(releaseR.getCode(),releaseR.getMsg());
        }
    }
}
