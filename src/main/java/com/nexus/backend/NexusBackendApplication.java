package com.nexus.backend;

import com.nexus.backend.model.User;
import com.nexus.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class NexusBackendApplication {

    private static final Logger log = LoggerFactory.getLogger(NexusBackendApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NexusBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                if (!userRepository.existsByEmail("admin@nexus.com")) {
                    User admin = new User();
                    admin.setName("Admin");
                    admin.setEmail("admin@nexus.com");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setRole(User.Role.ADMIN);
                    userRepository.save(admin);
                    log.info("Admin user seeded successfully");
                }
            } catch (Exception e) {
                log.warn("Admin seed skipped: {}", e.getMessage());
            }
        };
    }
}
