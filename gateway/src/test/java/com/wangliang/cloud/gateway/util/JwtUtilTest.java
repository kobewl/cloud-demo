package com.wangliang.cloud.gateway.util;

import com.wangliang.cloud.gateway.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    @Test
    void missingSecretFailsFast() {
        assertThatThrownBy(() -> new JwtUtil(props(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtUtil(props("   ")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtUtil(props("too-short")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    private static JwtProperties props(String secret) {
        JwtProperties p = new JwtProperties();
        p.setSecret(secret);
        return p;
    }
}
