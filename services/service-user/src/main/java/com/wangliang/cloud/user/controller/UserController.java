package com.wangliang.cloud.user.controller;

import com.wangliang.cloud.common.core.api.R;
import com.wangliang.cloud.common.core.api.ResultCode;
import com.wangliang.cloud.user.util.JwtUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户服务接口：登录签发 token（P15 JWT A）。
 * 最小可用版：不连数据库，账号密码硬编码校验 —— 重点是走通"签发 token"链路。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 登录接口：校验账号密码，通过则签发 JWT。
     * 请求体示例：{"username": "admin", "password": "123456"}
     */
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return R.fail(ResultCode.PARAM_ERROR);
        }
        if (!username.equals("admin") || !password.equals("123456")) {
            return R.fail(ResultCode.INVALID_CREDENTIALS);
        }
        // 🔴 RBAC 第 1 步（与 JwtUtil.generateToken 同步改）：调用处加第三个实参 "ADMIN"
        //   （真实项目里 roles 查自数据库用户表；学习项目 admin 账号先写死）
        return R.ok(Map.of("token", JwtUtil.generateToken(1L, username, "ADMIN")));
    }
}
