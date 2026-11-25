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
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单的"全局事务本体"：查商品 → 扣库存 → 落订单。
 * <p>
 * 为什么单独拆成一个 Bean，而不是写在 OrderServiceImpl 里？
 * {@code @GlobalTransactional} 和 {@code @Transactional} 一样靠 Spring AOP 代理生效，
 * 同一个类里 this.xxx() 自调用会绕过代理，注解就失效了（面试高频坑）。
 * 幂等判断在 OrderServiceImpl（事务外），真正的事务在这里（事务内）。
 * <p>
 * 为什么用 @GlobalTransactional 而不是手写补偿：
 * <ol>
 *   <li>手写补偿只在"业务失败"时能兜住，进程崩溃（kill -9）时补偿代码跟着没了；</li>
 *   <li>补偿调用本身也可能失败（库存服务恰好挂了）；</li>
 *   <li>补偿时机可能误判（落单其实成功只是响应超时）。</li>
 * </ol>
 * Seata 的解法：TM 开全局事务 → XID 经 Feign 头传递 → 每个 RM 写 undo_log 照片 →
 * 任一步抛异常由进程外的 TC 自动逆向回滚（崩溃也能靠心跳超时发现）。
 */
@Component
@RequiredArgsConstructor
public class OrderCreateTx {

    /** 商品服务 Feign 客户端：下单时查商品 */
    private final ProductFeignClient productFeignClient;

    /** 库存服务 Feign 客户端：下单时扣库存 */
    private final StockFeignClient stockFeignClient;

    private final OrderMapper orderMapper;

    /**
     * 在全局事务里创建订单。任何异常（包括唯一索引冲突 DuplicateKeyException）都会触发全局回滚，
     * 已扣的库存由 TC 按 undo_log 自动加回，然后把原始异常抛给调用方。
     *
     * @return 新订单 ID
     */
    @GlobalTransactional(timeoutMills = 30000, name = "order-create")
    public Long execute(OrderCreateDTO dto) {
        // ① 查商品（Feign）：返回 R，先看错误码
        R<ProductInfoDTO> productR = productFeignClient.getProduct(dto.getProductId());
        if (productR.getCode() != 0) {
            // 商品服务返回失败：透传原始 code/msg（可能是商品不存在、商品服务不可用等）
            throw new BusinessException(productR.getCode(), productR.getMsg());
        }
        if (productR.getData() == null) {
            // 兜底：返回成功但没数据，说明商品不存在
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // ② 预占库存（Feign）：先生成订单 ID，按该 ID 预占，失败则透传错误码。
        // 注意：这里是"分支事务"，库存服务的本地提交先做了，但 TC 记着账；
        // 如果后面任一步失败，TC 会拿着 undo_log 照片把库存自动加回。
        // 超时的情况也兜得住：超时 → fallback 返回失败 → 这里抛异常 → 全局回滚，
        // 库存分支哪怕"超时后才扣成功"，TC 也会回滚它（或它注册分支时发现全局事务已回滚而本地回滚）。
        Long orderId = IdWorker.getId();
        R<Void> holdR = stockFeignClient.holdStock(orderId, dto.getProductId(), dto.getCount());
        if (holdR.getCode() != 0) {
            throw new BusinessException(holdR.getCode(), holdR.getMsg());
        }

        // ③ 组装订单：商品信息快照 + 总金额 = 单价 x 数量
        ProductInfoDTO product = productR.getData();
        Order order = new Order();
        order.setId(orderId);
        order.setRequestId(dto.getRequestId());
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setPrice(product.getPrice());
        order.setCount(dto.getCount());
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(dto.getCount())));
        order.setStatus(0);
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setExpireAt(now.plusMinutes(5));

        // ④ 落订单：request_id 撞唯一索引会抛 DuplicateKeyException，同样触发全局回滚（库存加回）
        if (orderMapper.insert(order) != 1) {
            throw new BusinessException(ResultCode.ORDER_CREATE_FAIL);
        }
        return order.getId();
    }
}
