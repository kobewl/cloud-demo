package com.wangliang.cloud.gateway.filter;

import com.wangliang.cloud.gateway.config.JwtProperties;
import com.wangliang.cloud.gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthGlobalFilterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123";

    private AuthGlobalFilter filter;

    /** 记录被放行到下游的请求；为 null 表示被网关拦下 */
    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        filter = new AuthGlobalFilter(new JwtUtil(props));
    }

    @Test
    void whiteListPathStripsSpoofedIdentityHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/user/login")
                .header(AuthGlobalFilter.HEADER_USER_ID, "999")
                .header(AuthGlobalFilter.HEADER_USER_ROLES, "ADMIN"));

        filter.filter(exchange, chain).block();

        HttpHeaders downstream = forwarded.get().getRequest().getHeaders();
        assertThat(downstream.containsKey(AuthGlobalFilter.HEADER_USER_ID)).isFalse();
        assertThat(downstream.containsKey(AuthGlobalFilter.HEADER_USER_ROLES)).isFalse();
    }

    @Test
    void missingTokenIsRejectedEvenWithSpoofedHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/product/admin/x")
                .header(AuthGlobalFilter.HEADER_USER_ROLES, "ADMIN"));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidTokenIsRejected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/order/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = token("other-secret-other-secret-other-secret-99", "1", "ADMIN");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/order/list")
                .header(HttpHeaders.AUTHORIZATION, forged));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validTokenOverridesSpoofedHeadersWithTokenClaims() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/order/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SECRET, "42", "USER"))
                .header(AuthGlobalFilter.HEADER_USER_ID, "1")
                .header(AuthGlobalFilter.HEADER_USER_ROLES, "ADMIN"));

        filter.filter(exchange, chain).block();

        HttpHeaders downstream = forwarded.get().getRequest().getHeaders();
        assertThat(downstream.get(AuthGlobalFilter.HEADER_USER_ID)).containsExactly("42");
        assertThat(downstream.get(AuthGlobalFilter.HEADER_USER_ROLES)).containsExactly("USER");
    }

    @Test
    void rawTokenWithoutBearerPrefixStillWorks() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/order/list")
                .header(HttpHeaders.AUTHORIZATION, token(SECRET, "7", "ADMIN")));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(AuthGlobalFilter.HEADER_USER_ID)).isEqualTo("7");
    }

    @Test
    void extractTokenHandlesBlankAndPrefix() {
        assertThat(AuthGlobalFilter.extractToken(null)).isNull();
        assertThat(AuthGlobalFilter.extractToken("  ")).isNull();
        assertThat(AuthGlobalFilter.extractToken("Bearer ")).isNull();
        assertThat(AuthGlobalFilter.extractToken("bearer abc")).isEqualTo("abc");
        assertThat(AuthGlobalFilter.extractToken("abc")).isEqualTo("abc");
    }

    private static String token(String secret, String userId, String roles) {
        return Jwts.builder()
                .subject(userId)
                .claim("roles", roles)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
