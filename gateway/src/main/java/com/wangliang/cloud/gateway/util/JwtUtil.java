package com.wangliang.cloud.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 网关侧 JWT 解析工具（P15 JWT B / TODO 3）。
 *
 * 为什么网关要自己写一份、不能 import 用户服务的 JwtUtil？
 * 微服务之间不共享代码 —— gateway 不依赖 service-user 模块。
 * 唯一纽带是【同一把密钥】：两边 SECRET 必须一字不差。
 */
public class JwtUtil {

    // TODO 1：把用户服务 JwtUtil 里的 SECRET 和 KEY 原样抄过来
    //   （IO 板块：io.jsonwebtoken 的 Jwts / Keys / SecretKey 等按需 import）
    private static final String SECRET = "12345678901234567890123456789012";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRE_MS = 2 * 60 * 60 * 1000;
    /**
     * TODO 2：验签解析 token —— 逻辑与用户服务 JwtUtil.parseToken 相同：
     *   Jwts.parser().verifyWith(KEY).build() → parseSignedClaims(token).getPayload()
     *   ⚠️ 别忘了 try-catch 包住：token 过期/篡改/格式坏都会抛异常，
     *   异常时 return null（AuthGlobalFilter 拿到 null 就回 401）。
     *   网关侧这一个 try-catch 是刚需 —— 不然恶意请求能把网关打出 500。
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
