package com.nexus.backend.controller;

import com.nexus.backend.model.Notification;
import com.nexus.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Teacher fetches their notifications by email/role
    @GetMapping("/my")
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication auth) {
        String email = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(notificationService.getNotificationsForUser(email, "TEACHER"));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllRead(Authentication auth) {
        if (auth != null && auth.getName() != null) {
            notificationService.markAllAsReadByEmail(auth.getName());
        }
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // Keep old endpoint for backward compat
    @GetMapping("/teacher")
    public ResponseEntity<List<Notification>> getTeacherNotifications() {
        return ResponseEntity.ok(notificationService.getNotifications("TEACHER"));
    }

    // Student fetches their notifications by email/role
    @GetMapping("/student")
    public ResponseEntity<List<Notification>> getStudentNotifications(Authentication auth) {
        String email = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(notificationService.getNotificationsForUser(email, "STUDENT"));
    }

    @PatchMapping("/student/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllStudentRead(Authentication auth) {
        notificationService.markAllAsReadByEmail(auth.getName());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
