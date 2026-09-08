package com.nexus.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String email;

    private String loginId;

    private String username;

    private String password;

    private String role; // optional role hint; actual role is resolved from database

    private String deviceFingerprint; // optional unique device ID from client

    public String getEmail() {
        if (email != null && !email.isBlank()) return email.trim();
        if (loginId != null && !loginId.isBlank()) return loginId.trim();
        if (username != null && !username.isBlank()) return username.trim();
        return "";
    }
}

