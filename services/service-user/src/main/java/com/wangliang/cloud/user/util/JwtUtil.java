package com.wangliang.cloud.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：签发与解析 token（P15 JWT A）。
 * 依赖：jjwt 0.12.x（pom 里已配好 jjwt-api / jjwt-impl / jjwt-jackson）。
 *
 * 原理：HS256 对称签名 —— 签发和验证用**同一把密钥**，
 * 所以网关验签时必须持有与这里完全相同的密钥字符串（两边写死一致即可）。
 */
public class JwtUtil {

    // TODO 1：定义密钥常量 SECRET —— 自己编一串至少 32 个字符的字符串
    //   （HS256 要求密钥 ≥ 256 位 = 32 字节，短了运行时会直接报错），
    //   再用它生成密钥对象：Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))
     private static final String SECRET = "12345678901234567890123456789012";
     private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // TODO 2：定义过期时间常量 EXPIRE_MS（毫秒），建议 2 小时 = 2 * 60 * 60 * 1000
    private static final long EXPIRE_MS = 2 * 60 * 60 * 1000;
    /**
     * 签发 token。
     * TODO 3：用 Jwts.builder() 组装链式调用：
     *   - subject(userId.toString()) 放用户 id，可以再 .claim("username", username) 带上用户名；
     *   - .expiration(new Date(System.currentTimeMillis() + EXPIRE_MS)) 设过期时间；
     *   - .signWith(KEY) 指定签名密钥；
     *   - 最后 .compact() 得到 token 字符串返回。
     */
    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析 token（验签 + 取载荷）。
     * TODO 4：用 Jwts.parser().verifyWith(KEY).build() 构建解析器，
     *   再 parseSignedClaims(token).getPayload() 拿到 Claims（就是 token 里存的载荷）；
     *   注意：token 过期 / 签名不对 / 格式坏 都会抛异常 ——
     *   用 try-catch 包住，异常时返回 null（网关拿 null 就知道该回 401）。
     */
    public static Claims parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(KEY)
                .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims;
        } catch (Exception e) {
            return null;
        }
    }
}
