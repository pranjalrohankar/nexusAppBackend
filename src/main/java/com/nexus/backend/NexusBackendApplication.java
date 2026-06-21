package com.nexus.backend;

import com.nexus.backend.model.User;
import com.nexus.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class NexusBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail("admin@nexus.com")) {
                User admin = new User();
                admin.setName("Admin");
                admin.setEmail("admin@nexus.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(User.Role.ADMIN);
                userRepository.save(admin);
                System.out.println("✅ Admin seeded: admin@nexus.com / admin123");
            }
        };
    }
}
