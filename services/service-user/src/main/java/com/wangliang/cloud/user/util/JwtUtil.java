package com.wangliang.cloud.user.util;

import com.wangliang.cloud.user.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具类：签发与解析 token（P15 JWT A）。
 * 依赖：jjwt 0.12.x（pom 里已配好 jjwt-api / jjwt-impl / jjwt-jackson）。
 *
 * 原理：HS256 对称签名 —— 签发和验证用**同一把密钥**，
 * 所以网关验签时必须持有与这里完全相同的 jwt.secret。
 * 密钥从配置读（见 {@link JwtProperties}），启动时校验长度，没配直接启动失败。
 */
@Component
public class JwtUtil {

    /** HS256 要求密钥 ≥ 256 位 = 32 字节 */
    static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expire;

    public JwtUtil(JwtProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "未配置 jwt.secret：请设置环境变量 JWT_SECRET，或在 Nacos service-user.yaml 中配置 jwt.secret（须与网关一致）");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret 至少 " + MIN_SECRET_BYTES + " 字节，当前 " + bytes.length + " 字节");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expire = properties.getExpire();
    }

    /**
     * 签发 token：subject 放用户 id，claim 带用户名和角色（角色必须进 token，网关才解析得到）。
     */
    public String generateToken(Long userId, String username, String roles) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("roles", roles)
                .expiration(new Date(System.currentTimeMillis() + expire.toMillis()))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token（验签 + 取载荷）。过期 / 签名不对 / 格式坏都返回 null。
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
