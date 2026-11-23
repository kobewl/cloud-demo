package com.wangliang.cloud.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wangliang.cloud.order.dto.OrderCreateDTO;
import com.wangliang.cloud.order.entity.Order;
import com.wangliang.cloud.order.mapper.OrderMapper;
import com.wangliang.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现。
 * <p>
 * 下单 = 幂等判断（本类，事务外） + 全局事务（{@link OrderCreateTx}，事务内）。
 * <p>
 * 幂等的两道防线（面试考点："用户连点 / 超时重发会不会下两单"）：
 * <ol>
 *   <li><b>先查</b>：requestId 已落过单 → 直接返回那一单的 ID，不再扣库存。挡住绝大多数重复请求，且很便宜；</li>
 *   <li><b>唯一索引兜底</b>：两个重复请求同时到达，都查不到、都进了事务 → 只有一个能插入成功，
 *       另一个撞 uk_request_id 抛 DuplicateKeyException → Seata 全局回滚把它扣的库存加回 →
 *       这里捕获后返回第一单的 ID。</li>
 * </ol>
 * 为什么不只靠"先查"：查和插之间有时间窗，并发下两个请求都会查到"没有"（check-then-act 竞态）。
 * 为什么不只靠唯一索引：每个重复请求都要走一遍"扣库存 → 回滚"，白白占用库存行锁和 Seata 全局锁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderCreateTx orderCreateTx;

    @Override
    public Long create(OrderCreateDTO dto) {
        // 防线①：先查。命中说明是重复请求，返回同一个结果（幂等 = 多次调用效果与一次相同）
        Long existingId = baseMapper.selectIdByRequestId(dto.getRequestId());
        if (existingId != null) {
            log.info("重复下单请求，直接返回已有订单，requestId={}, orderId={}", dto.getRequestId(), existingId);
            return existingId;
        }
        try {
            return orderCreateTx.execute(dto);
        } catch (DataIntegrityViolationException e) {
            // 防线②：并发重复请求撞了唯一索引（MySQL 1062 → Spring 翻译成 DuplicateKeyException，
            // 它是 DataIntegrityViolationException 的子类；捕获父类，防止不同翻译器版本给出的子类不同）。
            // 走到这里时本次全局事务已回滚（库存已加回）
            Long winnerId = baseMapper.selectIdByRequestId(dto.getRequestId());
            if (winnerId == null) {
                // 撞的不是 request_id（理论上不会发生），不能吞掉
                throw e;
            }
            log.info("并发重复下单被唯一索引拦截，返回先到的订单，requestId={}, orderId={}",
                    dto.getRequestId(), winnerId);
            return winnerId;
        }
    }
}
