package com.wangliang.cloud.common.core.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter // 自动生成get方法
@AllArgsConstructor // 自动生成“传两个参数的构造函数”
public enum ResultCode {

    // 成功
    SUCCESS(0, "成功"),

    // 系统/通用错误
    SYSTEM_ERROR(70000, "系统繁忙，请稍后重试"),
    PARAM_ERROR(70001, "参数错误"),

    // 商品服务错误（701xx）
    PRODUCT_NOT_FOUND(70100, "商品不存在"),
    PRODUCT_SAVE_FAIL(70101, "商品保存失败"),
    PRODUCT_UPDATE_FAIL(70102, "商品更新失败"),
    PRODUCT_DELETE_FAIL(70103, "商品删除失败"),
    PRODUCT_LIST_FAIL(70104, "商品列表获取失败"),
    PRODUCT_DETAIL_FAIL(70105, "商品详情获取失败"),
    PRODUCT_SEARCH_FAIL(70106, "商品搜索失败"),
    PRODUCT_SORT_FAIL(70107, "商品排序失败"),
    PRODUCT_FILTER_FAIL(70108, "商品过滤失败"),

    // 库存服务错误（702xx）
    STOCK_NOT_FOUND(70200, "库存记录不存在"),
    STOCK_NOT_ENOUGH(70201, "库存不足"),
    STOCK_EXISTS(70202, "该商品已有库存记录"),

    // 订单服务错误（703xx）
    ORDER_CREATE_FAIL(70300, "订单创建失败"),
    ORDER_NOT_FOUND(70301, "订单不存在");


    // 错误码
    private final Integer code;

    // 错误信息
    private final String msg;
}
