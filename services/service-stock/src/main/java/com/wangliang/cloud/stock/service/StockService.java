package com.wangliang.cloud.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wangliang.cloud.stock.entity.Stock;

/**
 * 库存服务接口
 */
public interface StockService extends IService<Stock> {

    /** 按商品 ID 查询库存 */
    Stock getByProductId(Long productId);

    /**
     * 扣减库存
     * @return true=扣减成功；false=库存不足
     */
    boolean deduct(Long productId, Integer count);

    /**
     * 回补库存（补偿用）：扣库存成功但后续步骤失败时调用
     * @return true=回补成功
     */
    boolean add(Long productId, Integer count);
}
