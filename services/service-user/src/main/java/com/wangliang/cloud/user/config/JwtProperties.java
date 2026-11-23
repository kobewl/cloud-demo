package com.wangliang.cloud.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT 配置（用户服务侧：签发 token）。
 * <p>
 * 密钥不写死在代码里，来源：环境变量 JWT_SECRET 或 Nacos service-user.yaml 的 jwt.secret（都不进 git）。
 * 必须与网关的 jwt.secret 一致。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HMAC-SHA256 密钥，至少 32 字节 */
    private String secret;

    /** token 有效期 */
    private Duration expire = Duration.ofHours(2);
}
