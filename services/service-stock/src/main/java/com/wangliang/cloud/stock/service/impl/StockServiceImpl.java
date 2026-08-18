package com.wangliang.cloud.stock.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wangliang.cloud.stock.entity.Stock;
import com.wangliang.cloud.stock.mapper.StockMapper;
import com.wangliang.cloud.stock.service.StockService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 库存服务实现
 */
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    @Override
    public Stock getByProductId(Long productId) {
        // lambdaQuery：MyBatis-Plus 的"链式条件查询"，等价于 SELECT * FROM stock WHERE product_id = ?
        return lambdaQuery().eq(Stock::getProductId, productId).one();
    }

    @Override
    public boolean deduct(Long productId, Integer count) {
        // 原子扣减：交给 Mapper 里的手写 SQL（数据库行锁保证并发安全）
        return baseMapper.deductStock(productId, count) > 0;
    }

    @Override
    public boolean add(Long productId, Integer count) {
        // 原子回补：把扣掉的库存加回来
        return baseMapper.addStock(productId, count) > 0;
    }
}
