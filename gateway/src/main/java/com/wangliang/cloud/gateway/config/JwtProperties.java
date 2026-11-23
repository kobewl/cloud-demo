package com.wangliang.cloud.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（网关侧只需要验签密钥）。
 * <p>
 * 密钥不再写死在代码里（写死 = 进了 git = 所有能看代码的人都能伪造 token）。
 * 来源二选一，都不进 git：
 * <ul>
 *   <li>环境变量 JWT_SECRET（IDEA Run Configuration → Environment variables）；</li>
 *   <li>Nacos 的 gateway.yaml 里加 jwt.secret（Nacos 配置优先级高于本地 application.yml）。</li>
 * </ul>
 * 必须与 service-user 的 jwt.secret 一致（HS256 对称签名：签发和验签用同一把密钥）。
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HMAC-SHA256 密钥，至少 32 字节（256 位），不足时 jjwt 会拒绝 */
    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
