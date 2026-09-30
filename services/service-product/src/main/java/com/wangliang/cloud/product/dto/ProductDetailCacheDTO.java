package com.wangliang.cloud.product.dto;

import com.wangliang.cloud.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品详情缓存对象：商品信息 + 查询时的库存快照。
 *
 * <p>这是缓存数据契约，不等同于数据库实体，也不等同于 HTTP 返回的 Map。
 * 目前只提供结构，Cache Aside 的命中、回填、失效和库存一致性策略留给学习者实现。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailCacheDTO {

    private Product product;

    private StockInfoDTO stock;
}
