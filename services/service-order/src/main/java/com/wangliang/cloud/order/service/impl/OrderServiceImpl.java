package com.wangliang.cloud.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.order.dto.OrderCreateDTO;
import com.wangliang.cloud.order.dto.ProductInfoDTO;
import com.wangliang.cloud.order.entity.Order;
import com.wangliang.cloud.order.feign.ProductFeignClient;
import com.wangliang.cloud.order.feign.StockFeignClient;
import com.wangliang.cloud.order.mapper.OrderMapper;
import com.wangliang.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单服务实现。
 * 下单流程（跨服务，无法用本地 @Transactional 一把梭）：
 *   ① 查商品（Feign） -> ② 扣库存（Feign） -> ③ 落订单（本地事务）
 * 全流程包在 @GlobalTransactional 全局事务里：任一步抛异常，TC 自动回滚所有分支
 * （库存扣了会自动加回），无需手写补偿。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    /** 商品服务 Feign 客户端：下单时查商品 */
    private final ProductFeignClient productFeignClient;

    /** 库存服务 Feign 客户端：下单时扣库存 */
    private final StockFeignClient stockFeignClient;

    /**
     * 创建订单（全局事务）。
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
    @GlobalTransactional(timeoutMills = 30000, name = "order-create")
    @Override
    public Long create(OrderCreateDTO dto) {
        // ① 查商品（Feign）：返回 R，先看错误码
        R<ProductInfoDTO> productR = productFeignClient.getProduct(dto.getProductId());
        if (productR.getCode() != 0) {
            // 商品服务返回失败：透传原始 code/msg（可能是商品不存在等）
            throw new BusinessException(productR.getCode(), productR.getMsg());
        }
        if (productR.getData() == null) {
            // 兜底：返回成功但没数据，说明商品不存在
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // ② 扣库存（Feign）：同样先看错误码，库存不足时透传"库存不足"
        // 注意：这里是"分支事务"，库存服务的本地提交先做了，但 TC 记着账；
        // 如果后面任一步失败，TC 会拿着 undo_log 照片把库存自动加回。
        R<Void> deductR = stockFeignClient.deductStock(dto.getProductId(), dto.getCount());
        if (deductR.getCode() != 0) {
            throw new BusinessException(deductR.getCode(), deductR.getMsg());
        }

        // ③ 组装订单：商品信息快照 + 总金额 = 单价 x 数量
        ProductInfoDTO product = productR.getData();
        Order order = new Order();
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setPrice(product.getPrice());
        order.setCount(dto.getCount());
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(dto.getCount())));
        order.setStatus(0);

        // ④ 落订单：失败直接抛异常即可，无需手动 addStock 补偿
        //    —— Seata 的 TC 会自动回滚库存分支（这是和手写补偿的本质区别）
        if (!this.save(order)) {
            throw new BusinessException(ResultCode.ORDER_CREATE_FAIL);
        }

        // ⑤ 返回订单 id
        return order.getId();
    }
}
