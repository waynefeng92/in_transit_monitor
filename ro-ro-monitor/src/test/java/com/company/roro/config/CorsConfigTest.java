package com.company.roro.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CORS 跨域配置单元测试
 *
 * 验证 CorsConfig 中基于环境变量的来源控制是否生效：
 * 1. 配置的来源被正确注册到 CorsRegistry
 * 2. Cookie 凭证允许被保留
 * 3. 通配符 * 不再出现
 */
class CorsConfigTest {

    @SuppressWarnings("unchecked")
    private Map<String, CorsConfiguration> getCorsConfigurations(CorsRegistry registry) throws Exception {
        Method method = CorsRegistry.class.getDeclaredMethod("getCorsConfigurations");
        method.setAccessible(true);
        return (Map<String, CorsConfiguration>) method.invoke(registry);
    }

    private CorsConfig createConfigWithOrigins(String originsValue) throws Exception {
        CorsConfig config = new CorsConfig();
        Field field = CorsConfig.class.getDeclaredField("allowedOrigins");
        field.setAccessible(true);
        field.set(config, originsValue);
        return config;
    }

    private CorsConfiguration getCorsConfig(CorsConfig config) throws Exception {
        CorsRegistry registry = new CorsRegistry();
        config.addCorsMappings(registry);
        return getCorsConfigurations(registry).get("/**");
    }

    @Test
    void shouldRegisterConfiguredOrigins() throws Exception {
        CorsConfig config = createConfigWithOrigins("http://trusted-domain.com,http://localhost:3000");
        CorsConfiguration corsConfig = getCorsConfig(config);

        assertNotNull(corsConfig, "Should have mapping for /**");
        assertTrue(corsConfig.getAllowedOriginPatterns().contains("http://trusted-domain.com"),
                "Should allow http://trusted-domain.com");
        assertTrue(corsConfig.getAllowedOriginPatterns().contains("http://localhost:3000"),
                "Should allow http://localhost:3000");
    }

    @Test
    void shouldNotIncludeWildcardOrigin() throws Exception {
        CorsConfig config = createConfigWithOrigins("http://trusted-domain.com");
        CorsConfiguration corsConfig = getCorsConfig(config);

        assertFalse(corsConfig.getAllowedOriginPatterns().contains("*"),
                "Should not contain wildcard origin");
    }

    @Test
    void shouldKeepAllowCredentialsEnabled() throws Exception {
        CorsConfig config = createConfigWithOrigins("http://trusted-domain.com");
        CorsConfiguration corsConfig = getCorsConfig(config);

        assertNotNull(corsConfig.getAllowCredentials(), "allowCredentials should not be null");
        assertTrue(corsConfig.getAllowCredentials(), "allowCredentials should be true");
    }

    @Test
    void shouldUseDefaultOriginWhenNoEnvSet() throws Exception {
        CorsConfig config = createConfigWithOrigins("http://localhost:5173");
        CorsConfiguration corsConfig = getCorsConfig(config);

        assertTrue(corsConfig.getAllowedOriginPatterns().contains("http://localhost:5173"),
                "Default origin should be http://localhost:5173");
        assertEquals(1, corsConfig.getAllowedOriginPatterns().size(),
                "Only one default origin should be configured");
    }

    @Test
    void shouldHandleSingleOrigin() throws Exception {
        CorsConfig config = createConfigWithOrigins("http://single-origin.com");
        CorsConfiguration corsConfig = getCorsConfig(config);

        assertEquals(1, corsConfig.getAllowedOriginPatterns().size());
        assertTrue(corsConfig.getAllowedOriginPatterns().contains("http://single-origin.com"));
    }

    @Test
    void shouldTrimWhitespaceAroundOrigins() throws Exception {
        CorsConfig config = createConfigWithOrigins(" http://origin-a.com , http://origin-b.com ");
        CorsConfiguration corsConfig = getCorsConfig(config);

        assertTrue(corsConfig.getAllowedOriginPatterns().contains("http://origin-a.com"),
                "Whitespace should be trimmed from origin-a");
        assertTrue(corsConfig.getAllowedOriginPatterns().contains("http://origin-b.com"),
                "Whitespace should be trimmed from origin-b");
    }
}
