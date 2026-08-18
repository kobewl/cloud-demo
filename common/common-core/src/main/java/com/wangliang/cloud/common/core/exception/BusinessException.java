package com.wangliang.cloud.common.core.exception;

import com.wangliang.cloud.common.core.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常：业务规则不满足时抛出（如"商品不存在"），
 * 由 GlobalExceptionHandler 统一转成 R 返回给前端。
 * 支持两种构造：按错误码枚举（常用），或按自定义 code+msg（用于 Feign 错误透传）。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 对应的错误码 */
    private final Integer code;

    /** 对应的提示信息 */
    private final String message;

    /** 按错误码枚举构造 */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
        this.message = resultCode.getMsg();
    }

    /** 按自定义 code+msg 构造：用于透传下游服务（如库存服务）返回的错误 */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
