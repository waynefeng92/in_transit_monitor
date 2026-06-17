package com.company.roro.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderConfigTest {

    @Test
    void shouldEncodeAndMatchPassword() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "testPassword";
        String encoded = encoder.encode(rawPassword);

        assertTrue(encoder.matches(rawPassword, encoded));
    }

    @Test
    void shouldRejectWrongPassword() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("correctPassword");

        assertFalse(encoder.matches("wrongPassword", encoded));
    }
}
