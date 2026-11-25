package com.wangliang.cloud.stock.service;

import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.stock.entity.StockHold;
import com.wangliang.cloud.stock.mapper.StockHoldMapper;
import com.wangliang.cloud.stock.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 库存预占服务：按订单号做预占、确认、释放。
 * 条件扣减请用 {@link StockMapper#deductStock}，不要先查库存再改 quantity。
 */
@Service
@RequiredArgsConstructor
public class StockHoldService {

    private final StockHoldMapper stockHoldMapper;

    private final StockMapper stockMapper;

    @Transactional(rollbackFor = Exception.class)
    public void reserve(Long orderId, Long productId, Integer count){
        if(orderId == null || productId == null || count == null || count <= 0){
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        StockHold existing = stockHoldMapper.selectById(orderId);
        if(existing != null){
            // 同一订单不能换商品、换数量后再次预占
            if(!Objects.equals(existing.getProductId(), productId) || !Objects.equals(existing.getCount(),count)){
                throw new BusinessException(ResultCode.PARAM_ERROR);
            }

            Integer status = existing.getStatus();
            if (Objects.equals(status,0) || Objects.equals(status,1)){
                return;
            }

            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        // 库存足够才扣减
        if(stockMapper.deductStock(productId,count) <= 0){
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }

        StockHold hold = new StockHold();
        hold.setOrderId(orderId);
        hold.setProductId(productId);
        hold.setCount(count);
        hold.setStatus(0);
        LocalDateTime now = LocalDateTime.now();
        hold.setCreatedAt(now);
        hold.setUpdatedAt(now);
        stockHoldMapper.insert(hold);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        if (stockHoldMapper.confirmHold(orderId) > 0) {
            return;
        }

        StockHold existing = stockHoldMapper.selectById(orderId);
        if (existing != null && Objects.equals(existing.getStatus(), 1)) {
            return;
        }

        throw new BusinessException(ResultCode.PARAM_ERROR);
    }

    @Transactional(rollbackFor = Exception.class)
    public void release(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (stockHoldMapper.releaseHold(orderId) > 0) {
            StockHold hold = stockHoldMapper.selectById(orderId);
            if (hold == null || hold.getProductId() == null
                    || hold.getCount() == null || hold.getCount() <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR);
            }
            if (stockMapper.addStock(hold.getProductId(), hold.getCount()) != 1) {
                throw new BusinessException(ResultCode.STOCK_NOT_FOUND);
            }
            return;
        }
        StockHold existing = stockHoldMapper.selectById(orderId);
        if (existing != null && Objects.equals(existing.getStatus(), 2)) {
            return;
        }
        throw new BusinessException(ResultCode.PARAM_ERROR);
    }
}
