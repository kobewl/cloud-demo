package com.wangliang.cloud.order.feign;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.order.dto.ProductInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务 Feign 客户端。
 * name 必须是商品服务在 Nacos 里注册的服务名，
 * 调用时 Feign 会去 Nacos 找到 service-product 的真实地址，帮你发 HTTP 请求。
 */
@FeignClient(name = "service-product")
public interface ProductFeignClient {

    /**
     * 查商品：对应商品服务的 GET /api/product/{id}。
     */
    @GetMapping("/api/product/{id}")
    R<ProductInfoDTO> getProduct(@PathVariable("id") Long productId);
}
