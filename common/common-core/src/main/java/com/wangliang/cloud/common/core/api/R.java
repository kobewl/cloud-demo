package com.wangliang.cloud.common.core.api;

import lombok.Data;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {

    // 状态码：0=成功，非 0=失败
    private Integer code;

    // 提示信息
    private String msg;

    // 业务数据
    private T data;

    // 成功（无数据）
    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(ResultCode.SUCCESS.getMsg());
        r.setData(data);
        return r;
    }

    // 失败（按错误码枚举）
    public static <T> R<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMsg());
    }

    // 失败（自定义 code + msg，用于 Feign 错误透传）
    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
