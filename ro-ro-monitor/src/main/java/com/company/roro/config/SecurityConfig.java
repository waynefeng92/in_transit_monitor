package com.company.roro.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Spring Security 5.x 安全配置 (Session 认证模式)
 *
 * <p>使用 WebSecurityConfigurerAdapter 适配器模式，适用于 Spring Boot 2.7.x。</p>
 *
 * <h3>安全策略</h3>
 * <ul>
 *   <li>CORS: 使用 {@link CorsConfig} 提供的跨域配置</li>
 *   <li>CSRF: 禁用 (API 服务，由 SameSite=Lax Cookie 防护)</li>
 *   <li>Session: IF_REQUIRED 模式，最大 1 个并发会话</li>
 *   <li>认证: 基于 {@link UserDetailsService} 的 Session 登录</li>
 *   <li>密码: 复用 {@link PasswordEncoderConfig} 提供的 BCryptPasswordEncoder</li>
 * </ul>
 *
 * <h3>端点权限</h3>
 * <ul>
 *   <li>{@code /api/auth/login} — 允许匿名访问</li>
 *   <li>{@code /api/auth/logout} — 允许匿名访问</li>
 *   <li>{@code /actuator/health} — 允许匿名访问 (Docker 健康检查)</li>
 *   <li>{@code /api/**} — 需要认证</li>
 *   <li>其他 — 允许匿名访问</li>
 * </ul>
 *
 * @author roro-team
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired(required = false)
    private UserDetailsService userDetailsService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .and()
            .and()
            .authorizeRequests()
                .antMatchers("/api/auth/login").permitAll()
                .antMatchers("/api/auth/logout").permitAll()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/api/upload/**").authenticated()
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            .and()
            .httpBasic().disable()
            .formLogin().disable()
            .logout().disable();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (userDetailsService != null) {
            auth.userDetailsService(userDetailsService);
            // PasswordEncoder bean is already defined in PasswordEncoderConfig.java
            // Spring Security auto-resolves it when userDetailsService is set
        }
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
