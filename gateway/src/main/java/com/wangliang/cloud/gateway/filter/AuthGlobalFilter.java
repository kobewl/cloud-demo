package com.wangliang.cloud.gateway.filter;

import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import com.wangliang.cloud.gateway.util.JwtUtil;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;

/**
 * 网关统一验签过滤器（P15 JWT B）—— W6 的主角。
 *
 * GlobalFilter = 对经过网关的所有请求生效；
 * Ordered      = 过滤器优先级，数值越小越先执行（验签要尽量靠前）。
 *
 * 重要：网关不能 import 用户服务的 JwtUtil（服务之间不共享代码，也不依赖对方模块）！
 * 解析 token 需要在网关里用 jjwt 自己写解析代码，密钥字符串与用户服务完全一致。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 白名单：这些路径不需要登录即可访问。
     * 登录接口本身必须在列 —— 否则"没 token 就不让登录，登录才能拿 token"死循环。
     */
    private static final List<String> WHITE_LIST = List.of("/user/login");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // TODO 1（今晚）：取出请求路径 exchange.getRequest().getPath().value()，
        //   命中白名单 → 直接放行（把最后的 return 挪进 if 里）
        String path = exchange.getRequest().getPath().value();
        if (WHITE_LIST.contains(path)) {
            return chain.filter(exchange);
        }
        // TODO 2（今晚）：从请求头取 token，约定头名 "Authorization"
        //   exchange.getRequest().getHeaders().getFirst("Authorization")
        //   取不到 → 回 401：exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)
        //   然后 return exchange.getResponse().setComplete()
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // TODO 3（明天）：token 存在 → 验签解析。
        //   在 gateway 里新建 util/JwtUtil（密钥与用户服务一致），解析失败同样回 401
        Claims claims = JwtUtil.parseToken(token);
        if (claims == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        // TODO 4：解析成功 → 把 userId 塞进新请求头 "X-User-Id"
        //   传给下游（WebFlux 里要 mutate 重建请求）
        //   注意：mutate() 返回的是"builder"，必须接收返回值并 build()，
        //   否则那行代码等于什么都没发生（写完的请求头根本没塞进去）
        ServerHttpRequest newRequest = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .build();
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    @Override
    public int getOrder() {
        return -100; // 验签最先做：在路由转发、限流之前先搞清楚"你是谁"
    }
}
