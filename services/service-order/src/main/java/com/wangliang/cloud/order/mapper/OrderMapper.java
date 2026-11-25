package com.wangliang.cloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wangliang.cloud.order.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 订单 Mapper：继承 BaseMapper 后自动拥有增删改查方法，
 * 无需手写 SQL（这是 MyBatis-Plus 的核心便利）。
 * 逻辑删除由 @TableLogic + 全局配置自动处理，无需在 SQL 里手动加 deleted 条件。
 */
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 按幂等键查订单 ID（幂等下单用）。
     * <p>
     * 故意手写 SQL、不带 deleted 条件：唯一索引 uk_request_id 不区分是否逻辑删除，
     * 查询口径必须和唯一索引一致，否则"插入撞键了却查不到是谁"。
     *
     * @return 订单 ID；没有则返回 null
     */
    @Select("select id from orders where request_id = #{requestId} limit 1")
    Long selectIdByRequestId(@Param("requestId") String requestId);

    /**
     * 关闭一笔已到期的待付款订单。
     * 手写 SQL 不会自动带 @TableLogic，必须自己写 deleted = 0。
     * @return 1=刚关闭；0=未改到（已支付、已关闭、未到期、无截止时间、已删除或不存在）
     */
    @Update("update orders set status = 2, updated_at = now() " +
            "where id = #{id} and status = 0 and expire_at is not null and expire_at <= now() and deleted = 0")
    int closeExpired(@Param("id") Long id);

    @Select("select id from orders " +
            "where status = 0 and expire_at is not null " +
            "and expire_at <= now() and deleted = 0 " +
            "order by expire_at, id limit #{limit}")
    List<Long> listExpiredIds(@Param("limit") int limit);

    @Update("update orders set status = 1, updated_at = now() " +
            "where id = #{id} and status = 0 " +
            "and expire_at is not null and expire_at > now() " +
            "and deleted = 0")
    int payIfPending(@Param("id") Long id);
}
