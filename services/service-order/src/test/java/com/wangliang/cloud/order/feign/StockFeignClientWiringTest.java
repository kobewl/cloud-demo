package com.wangliang.cloud.order.feign;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.sun.net.httpserver.HttpServer;
import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证真实装配：Sentinel 包装 Feign + fallbackFactory + 超时配置 + 熔断资源名。
 * 用 JDK 自带 HttpServer 冒充库存服务，SimpleDiscoveryClient 代替 Nacos。
 */
@SpringBootTest(classes = StockFeignClientWiringTest.TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StockFeignClientWiringTest {

    /** docs/nacos-config/service-order-degrade.json 里写的资源名，必须和 Sentinel 实际生成的一致 */
    static final String DEDUCT_RESOURCE = "POST:http://service-stock/api/stock/deduct/{productId}/{count}";

    static final HttpServer STUB;
    static final AtomicInteger HITS = new AtomicInteger();
    /** 返回 "ok" / "slow" / "500" */
    static final AtomicReference<String> MODE = new AtomicReference<>("ok");

    static {
        try {
            STUB = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        STUB.createContext("/api/stock/", exchange -> {
            HITS.incrementAndGet();
            String mode = MODE.get();
            if ("slow".equals(mode)) {
                sleep(1500);
            }
            int status = "500".equals(mode) ? 500 : 200;
            byte[] body = "{\"code\":0,\"msg\":\"成功\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        STUB.start();
    }

    @DynamicPropertySource
    static void stockServiceAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.service-stock[0].uri",
                () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
    }

    @AfterAll
    static void stopStub() {
        STUB.stop(0);
    }

    @Autowired
    StockFeignClient stockFeignClient;

    @BeforeEach
    void reset() {
        HITS.set(0);
        MODE.set("ok");
        DegradeRuleManager.loadRules(Collections.emptyList());
    }

    @AfterEach
    void clearRules() {
        DegradeRuleManager.loadRules(Collections.emptyList());
    }

    @Test
    void healthyCallGoesThrough() {
        R<Void> r = stockFeignClient.deductStock(1L, 1);

        assertThat(r.getCode()).isZero();
        assertThat(HITS.get()).isEqualTo(1);
    }

    @Test
    void slowStockServiceTimesOutAndFallsBack() {
        MODE.set("slow");
        long start = System.currentTimeMillis();

        R<Void> r = stockFeignClient.deductStock(1L, 1);

        assertThat(System.currentTimeMillis() - start).isLessThan(1400); // read-timeout 500ms 生效，没等满 1.5s
        assertThat(r.getCode()).isEqualTo(ResultCode.SERVICE_UNAVAILABLE.getCode());
        assertThat(r.getMsg()).contains("SocketTimeoutException");
        assertThat(HITS.get()).isEqualTo(1); // 没有重试：扣库存不是幂等操作
    }

    @Test
    void circuitOpensAfterErrorsAndStopsCallingStockService() {
        DegradeRule rule = new DegradeRule(DEDUCT_RESOURCE)
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT)
                .setCount(2)
                .setMinRequestAmount(1)
                .setStatIntervalMs(10_000)
                .setTimeWindow(10);
        DegradeRuleManager.loadRules(List.of(rule));
        MODE.set("500");

        for (int i = 0; i < 3; i++) {
            stockFeignClient.deductStock(1L, 1);
        }
        int hitsBeforeOpen = HITS.get();
        R<Void> r = stockFeignClient.deductStock(1L, 1);

        assertThat(r.getMsg()).contains("熔断中");
        assertThat(HITS.get()).isEqualTo(hitsBeforeOpen); // 熔断期间请求根本没发出去
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @EnableFeignClients(clients = StockFeignClient.class)
    @Import(StockFeignFallbackFactory.class)
    static class TestApp {
    }
}
