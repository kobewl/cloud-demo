package com.wangliang.cloud.order.service.impl;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.order.dto.OrderCreateDTO;
import com.wangliang.cloud.order.dto.ProductInfoDTO;
import com.wangliang.cloud.order.entity.Order;
import com.wangliang.cloud.order.feign.ProductFeignClient;
import com.wangliang.cloud.order.feign.StockFeignClient;
import com.wangliang.cloud.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 全局事务本体：任何一步失败都必须抛异常（Seata 靠异常触发全局回滚） */
class OrderCreateTxTest {

    private ProductFeignClient productFeignClient;
    private StockFeignClient stockFeignClient;
    private OrderMapper orderMapper;
    private OrderCreateTx tx;

    @BeforeEach
    void setUp() {
        productFeignClient = mock(ProductFeignClient.class);
        stockFeignClient = mock(StockFeignClient.class);
        orderMapper = mock(OrderMapper.class);
        tx = new OrderCreateTx(productFeignClient, stockFeignClient, orderMapper);
    }

    @Test
    void savesOrderWithRequestIdAndAmount() {
        when(productFeignClient.getProduct(1L)).thenReturn(R.ok(product()));
        when(stockFeignClient.deductStock(1L, 3)).thenReturn(R.ok());
        when(orderMapper.insert(any(Order.class))).thenReturn(1);

        tx.execute(dto());

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(saved.capture());
        assertThat(saved.getValue().getRequestId()).isEqualTo("r1");
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("29.97");
    }

    @Test
    void stockServiceUnavailableThrowsSoGlobalTxRollsBack() {
        when(productFeignClient.getProduct(1L)).thenReturn(R.ok(product()));
        when(stockFeignClient.deductStock(1L, 3))
                .thenReturn(R.fail(ResultCode.SERVICE_UNAVAILABLE.getCode(), "库存服务暂不可用"));

        assertThatThrownBy(() -> tx.execute(dto()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SERVICE_UNAVAILABLE.getCode());
        verifyNoInteractions(orderMapper);
    }

    @Test
    void productServiceUnavailableStopsBeforeDeductingStock() {
        when(productFeignClient.getProduct(1L))
                .thenReturn(R.fail(ResultCode.SERVICE_UNAVAILABLE.getCode(), "商品服务暂不可用"));

        assertThatThrownBy(() -> tx.execute(dto())).isInstanceOf(BusinessException.class);
        verifyNoInteractions(stockFeignClient, orderMapper);
    }

    private static OrderCreateDTO dto() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setRequestId("r1");
        dto.setProductId(1L);
        dto.setCount(3);
        return dto;
    }

    private static ProductInfoDTO product() {
        ProductInfoDTO p = new ProductInfoDTO();
        p.setId(1L);
        p.setName("book");
        p.setPrice(new BigDecimal("9.99"));
        return p;
    }
}
