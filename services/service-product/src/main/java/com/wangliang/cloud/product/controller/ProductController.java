package com.wangliang.cloud.product.controller;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.product.entity.Product;
import com.wangliang.cloud.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品接口：所有返回都是统一格式 R
 */
@RestController              // 1. 标记这是一个"接口控制器"，返回的数据自动转成 JSON
@RequestMapping("/api/product")  // 2. 这个类的所有接口路径都带 /api/product 前缀
@RequiredArgsConstructor     // 3. Lombok：为 final 字段自动生成构造器（Spring 通过构造器注入依赖）
public class ProductController {

    private final ProductService productService;   // 4. 注入服务（final + @RequiredArgsConstructor = 官方推荐的构造器注入）

    /** 查询商品列表 */
    @GetMapping("/list")
    public R<List<Product>> list() {
        return R.ok(productService.list());        // productService.list() 是 MyBatis-Plus 提供的全表查询
    }

    /** 按 ID 查询商品 */
    @GetMapping("/{id}")
    public R<Product> getById(@PathVariable Long id) {   // @PathVariable：把 URL 里的 {id} 传给参数
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);   // 业务异常交给全局处理器兜底
        }
        return R.ok(product);
    }

    /** 新增商品 */
    @PostMapping
    public R<Void> save(@RequestBody Product product) {   // @RequestBody：把请求体 JSON 自动转成 Product 对象
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
}
