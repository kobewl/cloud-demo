package com.wangliang.cloud.product.controller;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.product.dto.StockInfoDTO;
import com.wangliang.cloud.product.entity.Product;
import com.wangliang.cloud.product.feign.StockFeignClient;
import com.wangliang.cloud.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品接口：所有返回都是统一格式 R
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** Feign 客户端：注入后就能远程调用库存服务（Spring 自动生成实现类） */
    private final StockFeignClient stockFeignClient;

    /** 查询商品列表 */
    @GetMapping("/list")
    public R<List<Product>> list() {
        return R.ok(productService.list());
    }

    /** 按 ID 查询商品 */
    @GetMapping("/{id}")
    public R<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return R.ok(product);
    }

    /** 新增商品 */
    @PostMapping
    public R<Void> save(@RequestBody Product product) {
        product.setCreatedAt(LocalDateTime.now());
        productService.save(product);
        return R.ok();
    }

    /** 删除商品 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productService.removeById(id);
        return R.ok();
    }

    /**
     * 商品详情（含库存）：演示 Feign 远程调用。
     * 商品服务把查库存的"活"交给 Feign，由它去库存服务把库存数据取回来。
     */
    @GetMapping("/{id}/detail")
    public R<Map<String, Object>> detailWithStock(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        // Feign 远程调用库存服务（看起来像本地方法，实际发起了 HTTP 请求）
        R<StockInfoDTO> stockR = stockFeignClient.getStock(id);
        // 若库存服务返回失败，把原始错误透传给前端（不吞错误）
        if (stockR.getCode() != 0) {
            return R.fail(stockR.getCode(), stockR.getMsg());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("stock", stockR.getData());
        return R.ok(result);
    }

    /**
     * 购买商品：查库存 -> 扣库存，完整走一遍微服务通信。
     */
    @PostMapping("/{id}/buy")
    public R<Void> buy(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        // Feign 调用库存服务扣减 1 件
        R<Void> stockR = stockFeignClient.deductStock(id, 1);
        if (stockR.getCode() != 0) {
            return R.fail(stockR.getCode(), stockR.getMsg());
        }
        return R.ok();
    }
}
