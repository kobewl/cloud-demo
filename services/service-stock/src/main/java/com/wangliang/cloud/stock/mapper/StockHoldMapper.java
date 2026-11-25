package com.wangliang.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wangliang.cloud.stock.entity.StockHold;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存预占 Mapper：继承 BaseMapper 后自动拥有按主键增删改查。
 * 主键是 orderId（实体上 IdType.INPUT），插入前必须先 setOrderId。
 */
public interface StockHoldMapper extends BaseMapper<StockHold> {

    @Update("update stock_hold set status = 1, updated_at = now() where order_id = #{orderId} and status = 0")
    int confirmHold(@Param("orderId") Long orderId);

    @Update("update stock_hold set status = 2, updated_at = now() where order_id = #{orderId} and status = 0")
    int releaseHold(@Param("orderId") Long orderId);
}
