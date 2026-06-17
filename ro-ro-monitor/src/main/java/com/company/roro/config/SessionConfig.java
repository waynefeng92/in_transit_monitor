package com.company.roro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Session 配置类
 *
 * 作用：配置 Session Cookie 的安全属性，防止 XSS 和 CSRF 攻击。
 *
 * 配置项：
 * - httpOnly: true — 禁止 JavaScript 读取 Cookie，防止 XSS 窃取 Session
 * - sameSite: "Lax" — 限制跨站请求携带 Cookie，防止 CSRF
 * - secure: false — 本系统未启用 HTTPS，所以不强制安全传输
 * - maxAge: 1800 — Session 有效期为 30 分钟
 *
 * 对应 application-prod.yml 中的 server.servlet.session.cookie 配置，
 * 这里通过编程方式提供更精细的控制。
 */
@Configuration
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("RORO_SESSION");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(false);  // No HTTPS in this deployment
        serializer.setCookieMaxAge(1800);  // 30 minutes
        return serializer;
    }
}
