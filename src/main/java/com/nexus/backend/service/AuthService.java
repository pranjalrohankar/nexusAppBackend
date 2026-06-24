package com.nexus.backend.service;

import com.nexus.backend.dto.LoginRequest;
import com.nexus.backend.dto.LoginResponse;
import com.nexus.backend.model.User;
import com.nexus.backend.repository.UserRepository;
import com.nexus.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String expectedRole = request.getRole().toUpperCase();
        if (!user.getRole().name().equals(expectedRole)) {
            throw new RuntimeException("Access denied for role: " + request.getRole());
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getRole().name(), user.getName(), user.getEmail());
    }
}
