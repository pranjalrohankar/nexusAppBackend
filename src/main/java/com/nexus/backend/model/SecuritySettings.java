package com.nexus.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "security_settings")
@Data
@NoArgsConstructor
public class SecuritySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    private boolean sessionTimeout = false;
    private boolean loginAlerts = false;
    private boolean failedLoginAlerts = false;
}
