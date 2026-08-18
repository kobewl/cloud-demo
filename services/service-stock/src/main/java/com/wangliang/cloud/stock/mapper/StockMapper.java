package com.wangliang.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wangliang.cloud.stock.entity.Stock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存 Mapper：deductStock 是手写 SQL，保证并发下不超卖。
 */
public interface StockMapper extends BaseMapper<Stock> {

    /**
     * 原子扣减库存（防超卖）：把"检查库存够不够"和"扣减"合并成一条 SQL。
     *
     * @param productId 商品ID
     * @param count     扣减数量
     * @return 影响行数：1=扣减成功；0=库存不足（quantity >= count 不满足，没扣到）
     */
    @Update("update stock set quantity = quantity - #{count}, updated_at = now() where product_id = #{productId} and quantity >= #{count}")
    int deductStock(@Param("productId") Long productId, @Param("count") Integer count);

    /**
     * 回补库存：下单流程里"扣库存成功但落订单失败"时，把扣掉的库存加回来（手动补偿）。
     * 正常业务流程不需要，但学习分布式事务补偿时很有用。
     *
     * @param productId 商品ID
     * @param count     回补数量
     * @return 影响行数：1=回补成功；0=该商品没有库存记录
     */
    @Update("update stock set quantity = quantity + #{count}, updated_at = now() where product_id = #{productId}")
    int addStock(@Param("productId") Long productId, @Param("count") Integer count);
}
