package com.nexus.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private String role; // optional role hint; actual role is resolved from database

    private String deviceFingerprint; // optional unique device ID from client
}
