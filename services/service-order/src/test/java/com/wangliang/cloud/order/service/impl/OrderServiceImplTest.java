package com.wangliang.cloud.order.service.impl;

import com.wangliang.cloud.order.dto.OrderCreateDTO;
import com.wangliang.cloud.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 下单幂等：先查 + 唯一索引兜底 */
class OrderServiceImplTest {

    private OrderMapper orderMapper;
    private OrderCreateTx orderCreateTx;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderCreateTx = mock(OrderCreateTx.class);
        service = new OrderServiceImpl(orderCreateTx);
        // ServiceImpl 的 baseMapper 是 @Autowired 字段注入，单测里手动塞
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
    }

    @Test
    void firstRequestRunsGlobalTransaction() {
        when(orderMapper.selectIdByRequestId("r1")).thenReturn(null);
        when(orderCreateTx.execute(any())).thenReturn(100L);

        assertThat(service.create(dto("r1"))).isEqualTo(100L);
        verify(orderCreateTx).execute(any());
    }

    @Test
    void repeatedRequestReturnsExistingOrderWithoutDeductingStockAgain() {
        when(orderMapper.selectIdByRequestId("r1")).thenReturn(100L);

        assertThat(service.create(dto("r1"))).isEqualTo(100L);
        verifyNoInteractions(orderCreateTx);
    }

    @Test
    void concurrentDuplicateCaughtByUniqueIndexReturnsWinnerOrder() {
        // 第一次查：还没有（两个并发请求都查到"没有"）；撞键后再查：先到的那单已经落库
        when(orderMapper.selectIdByRequestId("r1")).thenReturn(null, 100L);
        when(orderCreateTx.execute(any())).thenThrow(new DuplicateKeyException("uk_request_id"));

        assertThat(service.create(dto("r1"))).isEqualTo(100L);
    }

    @Test
    void integrityViolationNotCausedByRequestIdIsRethrown() {
        when(orderMapper.selectIdByRequestId("r1")).thenReturn(null, null);
        DataIntegrityViolationException other = new DataIntegrityViolationException("some other constraint");
        when(orderCreateTx.execute(any())).thenThrow(other);

        assertThatThrownBy(() -> service.create(dto("r1"))).isSameAs(other);
    }

    private static OrderCreateDTO dto(String requestId) {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setRequestId(requestId);
        dto.setProductId(1L);
        dto.setCount(2);
        return dto;
    }
}
