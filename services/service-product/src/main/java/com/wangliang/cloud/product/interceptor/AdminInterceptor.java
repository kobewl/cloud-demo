package com.wangliang.cloud.product.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理接口角色拦截器（P15 JWT B / RBAC 第 3 步，🔴 核心逻辑由你填写）。
 *
 * 链路回顾：登录 → token 里带 roles → 网关解析后放进 "X-User-Roles" 请求头
 * → 请求到达本服务 → 本拦截器检查角色，不是 ADMIN 就回 403。
 *
 * ⚠️ 安全前提（面试考点）：X-User-Roles 是网关塞的，前提是"外人只能走网关进"。
 * 若有人绕过网关直连 8081 伪造这个头，拦截器就被骗了 ——
 * 所以生产上业务服务的端口只对内网开放，公网流量一律从网关过。
 */
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 🔴 TODO：从请求头取 "X-User-Roles"（request.getHeader(...)）
        //   取不到 / 不包含 "ADMIN" → response.setStatus(403) 并 return false（拦下）
        //   是 ADMIN → return true（放行）
        String roles = request.getHeader("X-User-Roles");
        if (roles == null || !roles.contains("ADMIN")) {
            response.setStatus(403);
            return false;
        }
        return true; // ← 占位放行：写完判断后，这行只能留在"是 ADMIN"的分支里
    }
}
