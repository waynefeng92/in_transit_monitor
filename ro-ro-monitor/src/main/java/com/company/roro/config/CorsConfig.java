package com.company.roro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 跨域配置类
 *
 * 作用：解决前后端分离开发时，浏览器禁止跨域请求的问题。
 *
 * 场景举例：
 * - 前端运行在 http://localhost:3000 (React/Vue)
 * - 后端运行在 http://localhost:8080 (Spring Boot)
 * - 如果没有这个配置，浏览器会拦截前端发来的请求，报 CORS 错误
 *
 * 来源配置：
 * - 通过环境变量 CORS_ALLOWED_ORIGINS 配置允许的跨域来源列表
 * - 多个来源用逗号分隔，例如：http://localhost:5173,http://192.168.1.100
 * - 默认值为 http://localhost:5173（开发环境）
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed.origins:http://localhost:5173}")
    private String allowedOrigins;

    /**
     * 配置跨域规则
     *
     * @param registry 跨域注册器，用于添加跨域规则
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}