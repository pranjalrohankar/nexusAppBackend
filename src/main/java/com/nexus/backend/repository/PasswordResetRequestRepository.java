package com.nexus.backend.repository;

import com.nexus.backend.model.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    List<PasswordResetRequest> findAllByOrderByCreatedAtDesc();
    List<PasswordResetRequest> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
