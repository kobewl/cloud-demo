package com.wangliang.cloud.product.controller;

import com.wangliang.cloud.common.core.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理接口验收工具（RBAC，纯模板已写好，不用改）。
 * GET /admin/ping 被 AdminInterceptor 保护 —— 只有 ADMIN 角色的 token 能进来；
 * 顺带验证网关透传：能读到网关塞进来的 X-User-Id 就说明透传链路通了。
 */
@RestController
@RequestMapping("/admin")
public class AdminTestController {

    @GetMapping("/ping")
    public R<String> ping(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return R.ok("欢迎管理员，网关透传的 userId = " + userId);
    }
}
