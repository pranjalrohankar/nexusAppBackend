package com.nexus.backend.repository;

import com.nexus.backend.model.SecuritySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Long> {
    Optional<SecuritySettings> findByUserId(Long userId);
}
