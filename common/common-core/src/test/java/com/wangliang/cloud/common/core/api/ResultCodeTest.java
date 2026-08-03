package com.wangliang.cloud.common.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ResultCode 错误码枚举测试
 */
class ResultCodeTest {

    @Test
    void success_成功码固定为0() {
        assertEquals(0, ResultCode.SUCCESS.getCode());
    }

    @Test
    void 商品不存在_错误码为70100() {
        assertEquals(70100, ResultCode.PRODUCT_NOT_FOUND.getCode());
        assertEquals("商品不存在", ResultCode.PRODUCT_NOT_FOUND.getMsg());
    }
}
