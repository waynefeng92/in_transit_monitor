package com.company.roro.config;

import com.company.roro.entity.User;
import com.company.roro.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminPassword = System.getenv("ADMIN_DEFAULT_PASSWORD");
        if (adminPassword == null || adminPassword.isEmpty()) {
            adminPassword = "admin123";
            log.warn("ADMIN_DEFAULT_PASSWORD not set, using default admin123");
        }

        User existing = userRepository.findByUsername("admin");
        if (existing == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            admin.setEnabled(true);
            userRepository.insert(admin);
            log.info("Admin user created with default password");
        } else {
            log.info("Admin user already exists");
        }
    }
}
