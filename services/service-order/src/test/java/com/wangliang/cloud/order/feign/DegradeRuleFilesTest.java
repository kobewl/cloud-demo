package com.wangliang.cloud.order.feign;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验 docs/nacos-config 里的熔断规则：JSON 能解析、规则合法、资源名对得上 Feign 方法。
 * 资源名写错不会报错，只会"规则静默不生效"，所以值得单独测。
 */
class DegradeRuleFilesTest {

    /** Sentinel 为 Feign 方法生成的资源名：METHOD:http://服务名 + 路径模板 */
    private static final Set<String> FEIGN_RESOURCES = Set.of(
            "POST:http://service-stock/api/stock/deduct/{productId}/{count}",
            "POST:http://service-stock/api/stock/add/{productId}/{count}",
            "GET:http://service-stock/api/stock/{productId}",
            "GET:http://service-product/api/product/{id}");

    @ParameterizedTest
    @ValueSource(strings = {"service-order-degrade.json", "service-product-degrade.json"})
    void ruleFileIsValid(String file) throws Exception {
        Path path = findNacosConfigDir().resolve(file);
        List<DegradeRule> rules = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .readValue(Files.readString(path), new TypeReference<>() { });

        assertThat(rules).isNotEmpty();
        assertThat(rules).allSatisfy(rule -> {
            assertThat(DegradeRuleManager.isValidRule(rule)).as("规则合法: %s", rule).isTrue();
            assertThat(FEIGN_RESOURCES).as("资源名对应某个 Feign 方法").contains(rule.getResource());
        });
    }

    /** 从当前工作目录向上找 docs/nacos-config：Maven（模块目录）和 IDEA（可能是项目根）都能跑 */
    private static Path findNacosConfigDir() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("docs/nacos-config");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("找不到 docs/nacos-config 目录");
    }
}
