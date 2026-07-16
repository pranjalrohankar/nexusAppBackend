package com.nexus.backend.controller;

import com.nexus.backend.model.Notification;
import com.nexus.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/teacher")
    public ResponseEntity<List<Notification>> getTeacherNotifications() {
        return ResponseEntity.ok(notificationService.getNotifications("TEACHER"));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllRead(@RequestParam String role) {
        notificationService.markAllAsRead(role);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
