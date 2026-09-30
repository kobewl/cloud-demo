package com.wangliang.cloud.product.config;

import com.wangliang.cloud.product.interceptor.AdminInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册管理接口拦截器（RBAC 第 3 步的装配，纯模板已写好）。
 * 拦截路径 /admin/** —— 以后真正的管理接口都放这个前缀下，自动受保护。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminInterceptor()).addPathPatterns("/admin/**");
    }
}
