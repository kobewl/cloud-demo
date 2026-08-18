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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单服务实现。
 * 下单流程（跨服务，无法用本地 @Transactional 一把梭）：
 *   ① 查商品（Feign） -> ② 扣库存（Feign） -> ③ 落订单（本地事务）
 *   ③ 失败时补偿：调库存服务把库存加回来。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    /** 商品服务 Feign 客户端：下单时查商品 */
    private final ProductFeignClient productFeignClient;

    /** 库存服务 Feign 客户端：下单时扣库存，落单失败时回补库存 */
    private final StockFeignClient stockFeignClient;

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

        // ④ 落订单：失败则补偿（把扣掉的库存加回来），再抛异常
        if (!this.save(order)) {
            stockFeignClient.addStock(dto.getProductId(), dto.getCount());
            throw new BusinessException(ResultCode.ORDER_CREATE_FAIL);
        }

        // ⑤ 返回订单 id
        return order.getId();
    }
}
