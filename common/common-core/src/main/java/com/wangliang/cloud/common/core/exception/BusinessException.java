package com.wangliang.cloud.common.core.exception;

import com.wangliang.cloud.common.core.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常：业务规则不满足时抛出（如"商品不存在"），
 * 由 GlobalExceptionHandler 统一转成 R 返回给前端。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 对应的错误码 */
    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }
}
