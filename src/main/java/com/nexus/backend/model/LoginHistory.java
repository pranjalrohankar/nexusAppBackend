package com.nexus.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Data
@NoArgsConstructor
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String device;
    private String browser;
    private String os;
    private String location;
    private String ipAddress;
    private String deviceFingerprint; // unique ID from client device
    private String status; // success | failed | current
    private LocalDateTime loginTime;
    private boolean active = true;
}
