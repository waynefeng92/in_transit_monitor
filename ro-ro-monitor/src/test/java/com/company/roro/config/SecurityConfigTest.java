package com.company.roro.config;

import com.company.roro.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Security 配置集成测试
 *
 * <p>验证 SecurityConfig 的端点权限控制：</p>
 * <ul>
 *   <li>/api/** 端点需要认证 → 未认证返回 401</li>
 *   <li>/actuator/health 允许匿名访问 → 返回 200</li>
 *   <li>/api/auth/login 允许匿名访问 → 不返回 401</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Minimal Spring Boot configuration for security testing only.
     * Does NOT load the main RoroMonitorApplication class (avoids DB dependency).
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})
    static class TestConfig {

        @Bean
        UserDetailsService userDetailsService() {
            UserDetails user = User.withUsername("admin")
                    .password("{noop}test123")
                    .roles("ADMIN")
                    .build();
            return new InMemoryUserDetailsManager(user);
        }

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    /**
     * Dummy controller to provide test endpoints for security verification.
     */
    @RestController
    static class TestController {

        @GetMapping("/api/transit/summary")
        public ResponseEntity<Map<String, String>> summary() {
            return ResponseEntity.ok(Map.of("status", "ok"));
        }

        @PostMapping("/api/auth/login")
        public ResponseEntity<Map<String, String>> login() {
            return ResponseEntity.ok(Map.of("result", "logged_in"));
        }
    }

    @Test
    void shouldRequireAuthForApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/transit/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowActuatorHealthWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowLoginWithoutAuth() throws Exception {
        // /api/auth/login is permitAll() — should NOT return 401
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().is2xxSuccessful());
    }
}
