package com.nexus.backend.repository;

import com.nexus.backend.model.Enrollment;
import com.nexus.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {

    List<Notification> findByReceiverRoleOrderByCreatedAtDesc(String receiverRole);
    long countByReceiverRoleAndSeenFalse(String receiverRole);

}