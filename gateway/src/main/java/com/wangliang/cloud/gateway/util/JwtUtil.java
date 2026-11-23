package com.wangliang.cloud.gateway.util;

import com.wangliang.cloud.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 网关侧 JWT 解析工具（P15 JWT B）。
 *
 * 为什么网关要自己写一份、不能 import 用户服务的 JwtUtil？
 * 微服务之间不共享代码 —— gateway 不依赖 service-user 模块。
 * 唯一纽带是【同一把密钥】：两边 jwt.secret 必须一字不差。
 *
 * 密钥从配置读取（见 {@link JwtProperties}），启动时校验：没配或太短直接启动失败，
 * 比"带着一把空密钥/弱密钥跑起来"安全（fail fast）。
 */
@Component
public class JwtUtil {

    /** HS256 要求密钥 ≥ 256 位 */
    static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;

    public JwtUtil(JwtProperties properties) {
        this.key = buildKey(properties.getSecret());
    }

    static SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "未配置 jwt.secret：请设置环境变量 JWT_SECRET，或在 Nacos gateway.yaml 中配置 jwt.secret（须与 service-user 一致）");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret 至少 " + MIN_SECRET_BYTES + " 字节，当前 " + bytes.length + " 字节");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 验签并解析 token。
     * token 过期 / 签名不对 / 格式坏都会抛异常，这里统一返回 null（AuthGlobalFilter 拿到 null 就回 401）。
     * 网关侧这个 try-catch 是刚需 —— 不然恶意请求能把网关打出 500。
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
