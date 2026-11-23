package com.wangliang.cloud.order.feign;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/** 降级只能返回"失败"，绝不能伪造"扣减成功" */
class FeignFallbackFactoryTest {

    @Test
    void stockDeductFallbackReturnsFailureOnTimeout() {
        R<Void> r = new StockFeignFallbackFactory().create(new SocketTimeoutException("Read timed out"))
                .deductStock(1L, 1);

        assertThat(r.getCode()).isEqualTo(ResultCode.SERVICE_UNAVAILABLE.getCode());
        assertThat(r.getMsg()).contains("SocketTimeoutException");
    }

    @Test
    void fallbackReportsRootCauseWhenFeignWrapsIt() {
        // Feign 把读超时包成 RetryableException(cause = SocketTimeoutException)
        Throwable wrapped = new RuntimeException("Read timed out executing POST", new SocketTimeoutException("Read timed out"));

        R<Void> r = new StockFeignFallbackFactory().create(wrapped).deductStock(1L, 1);

        assertThat(r.getMsg()).contains("SocketTimeoutException");
    }

    @Test
    void stockDeductFallbackReportsOpenCircuit() {
        R<Void> r = new StockFeignFallbackFactory().create(new DegradeException("circuit open"))
                .deductStock(1L, 1);

        assertThat(r.getCode()).isEqualTo(ResultCode.SERVICE_UNAVAILABLE.getCode());
        assertThat(r.getMsg()).contains("熔断中");
    }

    @Test
    void productFallbackReturnsFailure() {
        var r = new ProductFeignFallbackFactory().create(new RuntimeException("connection refused")).getProduct(1L);

        assertThat(r.getCode()).isEqualTo(ResultCode.SERVICE_UNAVAILABLE.getCode());
        assertThat(r.getData()).isNull();
    }
}
