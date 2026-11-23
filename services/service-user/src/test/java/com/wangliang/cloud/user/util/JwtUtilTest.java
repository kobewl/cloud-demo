package com.wangliang.cloud.user.util;

import com.wangliang.cloud.user.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123";

    @Test
    void generatedTokenCarriesUserIdAndRoles() {
        JwtUtil jwt = new JwtUtil(props(SECRET, Duration.ofMinutes(5)));

        Claims claims = jwt.parseToken(jwt.generateToken(1L, "admin", "ADMIN"));

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("roles", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtil jwt = new JwtUtil(props(SECRET, Duration.ofSeconds(-1)));

        assertThat(jwt.parseToken(jwt.generateToken(1L, "admin", "ADMIN"))).isNull();
    }

    @Test
    void missingOrShortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtUtil(props(null, Duration.ofHours(2))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtUtil(props("short", Duration.ofHours(2))))
                .isInstanceOf(IllegalStateException.class);
    }

    private static JwtProperties props(String secret, Duration expire) {
        JwtProperties p = new JwtProperties();
        p.setSecret(secret);
        p.setExpire(expire);
        return p;
    }
}
