package com.wangliang.cloud.common.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R 统一返回体测试：验证成功/失败的返回格式
 */
class RTest {

    @Test
    void ok_成功时code为0且带数据() {
        R<String> r = R.ok("你好");
        assertEquals(0, r.getCode());
        assertEquals("成功", r.getMsg());
        assertEquals("你好", r.getData());
    }

    @Test
    void fail_按错误码返回() {
        R<Void> r = R.fail(ResultCode.PRODUCT_NOT_FOUND);
        assertEquals(70100, r.getCode());
        assertEquals("商品不存在", r.getMsg());
    }
}
