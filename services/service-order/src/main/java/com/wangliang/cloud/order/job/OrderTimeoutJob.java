package com.wangliang.cloud.order.job;

import com.wangliang.cloud.order.mapper.OrderMapper;
import com.wangliang.cloud.order.service.impl.OrderTimeoutTx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 超时关单扫描：查出到期候选，逐单调用 {@link OrderTimeoutTx#close}。
 * 先不要加 {@code @Scheduled}，等表结构和 Seata 核验后再开。
 */
@Slf4j
@Component
public class OrderTimeoutJob {

    private final OrderMapper orderMapper;

    private final OrderTimeoutTx orderTimeoutTx;
    private final ThreadPoolTaskExecutor orderTimeoutExecutor;


    public OrderTimeoutJob(
            OrderMapper orderMapper,
            OrderTimeoutTx orderTimeoutTx,
            @Qualifier("orderTimeoutExecutor")
            ThreadPoolTaskExecutor orderTimeoutExecutor) {
        this.orderMapper = orderMapper;
        this.orderTimeoutTx = orderTimeoutTx;
        this.orderTimeoutExecutor = orderTimeoutExecutor;
    }

    @Scheduled(fixedDelay = 10000)
    public void scan() {
        List<Long> ids = orderMapper.listExpiredIds(100);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Long orderId : ids) {
            try {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        orderTimeoutTx.close(orderId);
                    } catch (Exception e){
                        log.error("超时关单失败，orderId:{}", orderId, e);
                    }
                }, orderTimeoutExecutor);
                futures.add(future);
            } catch (Exception e){
                log.error("超时关单失败，orderId={}", orderId, e);
                break;
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    }
}
