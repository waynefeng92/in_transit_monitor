package com.company.roro.controller;

import com.company.roro.config.PasswordEncoderConfig;
import com.company.roro.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth integration test — verifies all protected endpoints enforce authentication.
 *
 * <p>Uses a minimal Spring Boot context (no DB/MyBatis) with a test controller
 * to verify the security filter chain works correctly.</p>
 *
 * <h3>Tests</h3>
 * <ol>
 *   <li>{@link #allProtectedEndpointsRequireAuth()} — unprotected requests to /api/** → 401</li>
 *   <li>{@link #authenticatedUserCanAccessProtectedEndpoints()} — @WithMockUser → 200</li>
 * </ol>
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
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Minimal config: SecurityConfig + PasswordEncoder + a test controller.
     * DB/MyBatis auto-config is excluded to avoid needing a real database.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, PasswordEncoderConfig.class})
    static class TestConfig {

        @RestController
        static class AuthTestController {
            @GetMapping("/api/auth-test-ping")
            public String ping() {
                return "pong";
            }

            @PostMapping("/api/auth-test-post")
            public String post() {
                return "ok";
            }
        }
    }

    // ──────────────────────────────────────────────
    // Test: Unauthenticated → 401 on all protected endpoints
    // ──────────────────────────────────────────────
    @Test
    void allProtectedEndpointsRequireAuth() throws Exception {
        // Real API endpoints — return 401 before any controller is invoked
        mockMvc.perform(get("/api/transit"))
               .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/brand"))
               .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/port"))
               .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/route"))
               .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/arrived"))
               .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/order"))
               .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/chart"))
               .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/upload/preview"))
               .andExpect(status().isUnauthorized());

        // Test controller endpoint is also protected under /api/
        mockMvc.perform(get("/api/auth-test-ping"))
               .andExpect(status().isUnauthorized());

        // ── Public endpoints should NOT be blocked ──
        // Login endpoint is permitAll (reaches controller or returns 400, not 401)
        var loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();
        assertNotEquals(401, loginResp.getResponse().getStatus(),
                "/api/auth/login should be accessible without authentication");
    }

    // ──────────────────────────────────────────────
    // Test: Authenticated → 200 on protected endpoints
    // ──────────────────────────────────────────────
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void authenticatedUserCanAccessProtectedEndpoints() throws Exception {
        // With mock user, security filter allows the request through
        mockMvc.perform(get("/api/auth-test-ping"))
               .andExpect(status().isOk())
               .andExpect(content().string("pong"));

        mockMvc.perform(post("/api/auth-test-post"))
               .andExpect(status().isOk())
               .andExpect(content().string("ok"));
    }
}
