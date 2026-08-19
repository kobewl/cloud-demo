package com.wangliang.cloud.stock.controller;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.common.core.exception.BusinessException;
import com.wangliang.cloud.stock.entity.Stock;
import com.wangliang.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 库存接口：商品服务通过 Feign 调这里的接口
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /** 新增/初始化库存（给商品配库存，测试用） */
    @PostMapping
    public R<Void> save(@RequestBody Stock stock) {
        // 一个商品只能有一条库存记录（product_id 唯一索引）。
        // 先查再插，重复配库存时返回明确提示，而不是让数据库抛重复键异常
        if (stockService.getByProductId(stock.getProductId()) != null) {
            throw new BusinessException(ResultCode.STOCK_EXISTS);
        }
        stock.setUpdatedAt(LocalDateTime.now());
        stockService.save(stock);
        return R.ok();
    }

    /** 按商品 ID 查询库存（Feign 调用入口） */
    @GetMapping("/{productId}")
    public R<Stock> getByProductId(@PathVariable Long productId) {
        Stock stock = stockService.getByProductId(productId);
        if (stock == null) {
            throw new BusinessException(ResultCode.STOCK_NOT_FOUND);
        }
        return R.ok(stock);
    }

    /** 扣减库存（Feign 调用入口） */
    @PostMapping("/deduct/{productId}/{count}")
    public R<Void> deduct(@PathVariable Long productId, @PathVariable Integer count) {
        if (!stockService.deduct(productId, count)) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
        return R.ok();
    }

    /** 回补库存（下单补偿用，Feign 调用入口） */
    @PostMapping("/add/{productId}/{count}")
    public R<Void> add(@PathVariable Long productId, @PathVariable Integer count) {
        if (!stockService.add(productId, count)) {
            throw new BusinessException(ResultCode.STOCK_NOT_FOUND);
        }
        return R.ok();
    }
}
