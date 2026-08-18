package com.wangliang.cloud.product.controller;

import com.wangliang.cloud.common.core.api.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置中心演示接口：验证"改配置不重启，实时生效"。
 *
 * @RefreshScope 的作用：Nacos 上的配置变更被推送后，
 * Spring 会自动销毁并重建这个 Bean，重新注入 @Value 的值。
 * 类比：整容器 refresh（重启）太重，这个注解只对挂上它的 Bean 做"定向刷新"。
 */
@RestController
@RequestMapping("/api/config")
@RefreshScope
public class ConfigController {

    /** 店铺公告：值来自 Nacos 配置中心里的 shop.notice（冒号后面是兜底默认值） */
    @Value("${shop.notice:暂无公告}")
    private String notice;

    /** 返回当前公告：改 Nacos 里的 shop.notice 后，直接刷新此接口即可看到新值 */
    @GetMapping("/notice")
    public R<String> notice() {
        return R.ok(notice);
    }
}
