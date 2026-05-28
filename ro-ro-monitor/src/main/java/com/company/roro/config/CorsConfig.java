package com.company.roro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置类
 *
 * 作用：解决前后端分离开发时，浏览器禁止跨域请求的问题。
 *
 * 场景举例：
 * - 前端运行在 http://localhost:3000 (React/Vue)
 * - 后端运行在 http://localhost:8080 (Spring Boot)
 * - 如果没有这个配置，浏览器会拦截前端发来的请求，报 CORS 错误
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 配置跨域规则
     *
     * @param registry 跨域注册器，用于添加跨域规则
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                          // 允许跨域的接口路径，/** 表示所有接口都允许跨域
                .allowedOriginPatterns("*")                 // 允许哪些前端地址访问，* 表示所有地址（开发环境用，生产环境建议改为具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的 HTTP 请求方法
                .allowedHeaders("*")                        // 允许的请求头，* 表示所有请求头都允许
                .allowCredentials(true)                     // 是否允许携带 Cookie 和认证信息
                .maxAge(3600);                              // 预检请求的缓存时间（秒），3600秒内相同请求不重复检查
    }
}