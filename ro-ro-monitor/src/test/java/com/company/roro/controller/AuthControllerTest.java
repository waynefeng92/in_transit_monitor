package com.company.roro.controller;

import com.company.roro.config.PasswordEncoderConfig;
import com.company.roro.config.SecurityConfig;
import com.company.roro.entity.User;
import com.company.roro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController TDD tests — login, logout, session-check.
 *
 * <p>Uses InMemoryUserDetailsManager (no DB) + @MockBean UserRepository
 * so AuthController's direct UserRepository calls return mocked entities.</p>
 *
 * <h3>Test coverage</h3>
 * <ol>
 *   <li>Valid credentials → 200 with UserInfo</li>
 *   <li>Wrong password → code=401</li>
 *   <li>Nonexistent user → code=401</li>
 *   <li>Logout invalidates session → 200</li>
 *   <li>GET /me with valid session → 200 with UserInfo</li>
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
class AuthControllerTest {

    private static final String TEST_USERNAME = "admin";
    private static final String RAW_PASSWORD = "test123";
    private static final String WRONG_PASSWORD = "wrong";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    private User mockUser;

    /**
     * Minimal Spring Boot configuration for auth controller testing.
     * Excludes DB/MyBatis auto-config. Provides in-memory UserDetailsService.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, PasswordEncoderConfig.class, AuthController.class})
    static class TestConfig {

        @Bean
        UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
            UserDetails user = org.springframework.security.core.userdetails.User.builder()
                    .username(TEST_USERNAME)
                    .password(passwordEncoder.encode(RAW_PASSWORD))
                    .roles("ADMIN")
                    .build();
            return new InMemoryUserDetailsManager(user);
        }
    }

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername(TEST_USERNAME);
        mockUser.setRole("ADMIN");
        mockUser.setEnabled(true);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(mockUser);
        when(userRepository.findByUsername("unknown")).thenReturn(null);
    }

    // ──────────────────────────────────────────────
    // Test 1: Valid credentials
    // ──────────────────────────────────────────────
    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    // ──────────────────────────────────────────────
    // Test 2: Wrong password
    // ──────────────────────────────────────────────
    @Test
    void shouldRejectInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"" + WRONG_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    // ──────────────────────────────────────────────
    // Test 3: Nonexistent user
    // ──────────────────────────────────────────────
    @Test
    void shouldRejectNonexistentUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unknown\",\"password\":\"anypassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    // ──────────────────────────────────────────────
    // Test 4: Logout invalidates session
    // ──────────────────────────────────────────────
    @Test
    void shouldLogoutAndInvalidateSession() throws Exception {
        // Login first to get a valid session
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // Logout with the same session
        mockMvc.perform(post("/api/auth/logout")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ──────────────────────────────────────────────
    // Test 5: GET /me returns current user
    // ──────────────────────────────────────────────
    @Test
    void shouldReturnCurrentUserWhenAuthenticated() throws Exception {
        // Login first
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // GET /me with session
        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }
}
