package com.company.roro.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserWithRequiredFields() {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("encodedPassword");
        user.setRole("ADMIN");
        user.setEnabled(true);

        assertEquals("admin", user.getUsername());
        assertEquals("ADMIN", user.getRole());
        assertTrue(user.getEnabled());
    }

    @Test
    void shouldHaveDefaultRoleAdmin() {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("pass");
        user.setRole("ADMIN");

        assertEquals("ADMIN", user.getRole());
    }
}
