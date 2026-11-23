package com.wangliang.cloud.product.controller;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.product.cache.ProductInfoCache;
import com.wangliang.cloud.product.dto.StockInfoDTO;
import com.wangliang.cloud.product.entity.Product;
import com.wangliang.cloud.product.feign.StockFeignClient;
import com.wangliang.cloud.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ProductControllerTest {

    private ProductService productService;
    private StockFeignClient stockFeignClient;
    private ProductInfoCache cache;
    private ProductController controller;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        stockFeignClient = mock(StockFeignClient.class);
        cache = mock(ProductInfoCache.class);
        controller = new ProductController(productService, stockFeignClient, cache);
    }

    /** 回归：下单（在 service-order 扣库存）后，详情页必须看到最新库存 */
    @Test
    void cacheHitStillReturnsLiveStock() {
        when(cache.get(1L)).thenReturn(Optional.of(product()));
        when(stockFeignClient.getStock(1L)).thenReturn(R.ok(stock(10)), R.ok(stock(7)));

        assertThat(stockOf(controller.detailWithStock(1L))).isEqualTo(10);
        assertThat(stockOf(controller.detailWithStock(1L))).isEqualTo(7);
        verify(productService, never()).getById(anyLong());
    }

    @Test
    void cacheMissLoadsFromDbAndFillsCache() {
        Product product = product();
        when(cache.get(1L)).thenReturn(Optional.empty());
        when(productService.getById(1L)).thenReturn(product);
        when(stockFeignClient.getStock(1L)).thenReturn(R.ok(stock(5)));

        controller.detailWithStock(1L);

        verify(cache).put(product);
    }

    @Test
    void stockServiceUnavailableIsPassedThrough() {
        when(cache.get(1L)).thenReturn(Optional.of(product()));
        when(stockFeignClient.getStock(1L))
                .thenReturn(R.fail(ResultCode.SERVICE_UNAVAILABLE.getCode(), "库存服务暂不可用"));

        R<Map<String, Object>> r = controller.detailWithStock(1L);

        assertThat(r.getCode()).isEqualTo(ResultCode.SERVICE_UNAVAILABLE.getCode());
    }

    @Test
    void deleteEvictsCache() {
        controller.delete(1L);

        verify(productService).removeById(1L);
        verify(cache).evict(1L);
    }

    private static Integer stockOf(R<Map<String, Object>> r) {
        return ((StockInfoDTO) r.getData().get("stock")).getQuantity();
    }

    private static Product product() {
        Product p = new Product();
        p.setId(1L);
        p.setName("book");
        return p;
    }

    private static StockInfoDTO stock(int quantity) {
        StockInfoDTO s = new StockInfoDTO();
        s.setProductId(1L);
        s.setQuantity(quantity);
        return s;
    }
}
