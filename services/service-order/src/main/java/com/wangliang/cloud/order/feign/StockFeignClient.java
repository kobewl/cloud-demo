package com.wangliang.cloud.order.feign;

import com.wangliang.cloud.common.core.api.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 库存服务 Feign 客户端。
 * name 必须是库存服务在 Nacos 里注册的服务名，
 * 调用时 Feign 会去 Nacos 找到 service-stock 的真实地址，帮你发 HTTP 请求。
 */
@FeignClient(name = "service-stock")
public interface StockFeignClient {

    /**
     * 扣库存：对应库存服务的 POST /api/stock/deduct/{productId}/{count}。
     */
    @PostMapping("/api/stock/deduct/{productId}/{count}")
    R<Void> deductStock(@PathVariable("productId") Long productId, @PathVariable("count") Integer count);

    @PostMapping("/api/stock/add/{productId}/{count}")
    R<Void> addStock(@PathVariable("productId") Long productId, @PathVariable("count") Integer count);
}
