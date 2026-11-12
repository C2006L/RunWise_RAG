package com.runwise.config;

import com.runwise.interceptor.JwtInterceptor;
import com.runwise.admin.AdminInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置
 * 注册 JWT 拦截器、配置跨域
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Value("${runwise.upload-dir:uploads}")
    private String uploadDir;

    public WebMvcConfig(JwtInterceptor jwtInterceptor, AdminInterceptor adminInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor).addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/admin/**",       // 独立的管理员认证，不能使用跑者 token
                        "/api/auth/login",     // 登录接口不拦截
                        "/api/auth/login-password",
                        "/api/auth/register",
                        "/api/auth/refresh",   // 刷新token不拦截
                        "/api/qa/categories",
                        "/api/qa/hot",
                        "/api/injury/**",      // 伤病预防为公开只读知识库，不拦截
                        "/doc.html",           // Knife4j 文档
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/api-docs/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
