package com.wangliang.cloud.gateway.filter;

import com.wangliang.cloud.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关统一验签过滤器（P15 JWT B）。
 *
 * GlobalFilter = 对经过网关的所有请求生效；
 * Ordered      = 过滤器优先级，数值越小越先执行（验签要尽量靠前）。
 *
 * 处理顺序：
 * <ol>
 *   <li><b>先清洗身份头</b>：无论是否白名单，一律删掉客户端自带的 X-User-Id / X-User-Roles。
 *       这两个头只能由网关根据验签结果写入，下游服务（如 AdminInterceptor）是无条件信任它们的。
 *       不清洗的话，攻击者自己带上 "X-User-Roles: ADMIN" 就能冒充管理员（白名单路径更是直接透传）。</li>
 *   <li>白名单放行（登录接口）；</li>
 *   <li>取 Authorization 头（兼容 "Bearer xxx" 和裸 token），验签失败回 401；</li>
 *   <li>验签成功，把 userId / roles 写进身份头传给下游。</li>
 * </ol>
 *
 * ⚠ 清洗只能防"经过网关"的伪造。业务服务端口（8081~8084）若能被外部直连，伪造头照样生效 ——
 * 所以部署上业务服务只对内网开放，公网流量一律从网关进（见 docs/notes 面试追问修补笔记）。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 下游服务读取的用户 ID 头 */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 下游服务读取的角色头 */
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 白名单：这些路径不需要登录即可访问。
     * 登录接口本身必须在列 —— 否则"没 token 就不让登录，登录才能拿 token"死循环。
     */
    private static final List<String> WHITE_LIST = List.of("/user/login");

    private final JwtUtil jwtUtil;

    public AuthGlobalFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // ① 清洗：删掉客户端伪造的身份头（mutate() 返回 builder，必须接住返回值 build，否则等于没改）
        ServerHttpRequest sanitized = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_ROLES);
                })
                .build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitized).build();

        // ② 白名单直接放行（放行的是清洗后的请求）
        String path = sanitized.getPath().value();
        if (WHITE_LIST.contains(path)) {
            return chain.filter(sanitizedExchange);
        }

        // ③ 取 token 并验签
        String token = extractToken(sanitized.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return unauthorized(sanitizedExchange);
        }
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null || claims.getSubject() == null) {
            return unauthorized(sanitizedExchange);
        }

        // ④ 验签通过：写入身份头传给下游
        String roles = claims.get("roles", String.class);
        ServerHttpRequest authed = sanitized.mutate()
                .headers(headers -> {
                    headers.set(HEADER_USER_ID, claims.getSubject());
                    if (roles != null) {
                        headers.set(HEADER_USER_ROLES, roles);
                    }
                })
                .build();
        return chain.filter(sanitizedExchange.mutate().request(authed).build());
    }

    /** 兼容 "Bearer xxx"（RFC 6750 标准写法）和直接传 token 两种格式；空值返回 null */
    static String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        // 先只去掉前导空白再判断前缀：若先 trim()，"Bearer " 会变成 "Bearer" 被误当成 token
        String value = authorization.stripLeading();
        if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            value = value.substring(BEARER_PREFIX.length());
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100; // 验签最先做：在路由转发、限流之前先搞清楚"你是谁"
    }
}
