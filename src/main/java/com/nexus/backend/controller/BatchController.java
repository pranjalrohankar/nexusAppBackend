package com.nexus.backend.controller;

import com.nexus.backend.dto.BatchDto;
import com.nexus.backend.model.Batch;
import com.nexus.backend.model.Enrollment;
import com.nexus.backend.model.SecuritySettings;
import com.nexus.backend.model.Student;
import com.nexus.backend.repository.EnrollmentRepository;
import com.nexus.backend.repository.SecuritySettingsRepository;
import com.nexus.backend.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;
    private final EnrollmentRepository enrollmentRepository;
    private final SecuritySettingsRepository securitySettingsRepository;

    @PostMapping
    public ResponseEntity<Batch> createBatch(@RequestBody BatchDto request) {
        return ResponseEntity.ok(batchService.createBatch(request));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllBatches());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Batch> updateBatch(@PathVariable Long id, @RequestBody BatchDto request) {
        return ResponseEntity.ok(batchService.updateBatch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBatch(@PathVariable Long id) {
        batchService.deleteBatch(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/students")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getBatchStudents(@PathVariable Long id) {
        Batch batch = batchService.getBatchById(id);
        if (batch == null) {
            return ResponseEntity.notFound().build();
        }

        // Case-insensitive match + JOIN FETCH student to avoid lazy-load issues
        List<Enrollment> enrollments = enrollmentRepository.findByCourseTitleIgnoreCase(batch.getSelectCourse());

        List<Map<String, Object>> students = enrollments.stream().map(e -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("enrollmentId", e.getId());
            dto.put("courseTitle", e.getCourseTitle());
            dto.put("enrollmentDate", e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "");
            dto.put("paymentStatus", e.getPaymentStatus() != null ? e.getPaymentStatus() : "");
            Student s = e.getStudent();
            if (s != null) {
                dto.put("id", s.getId());
                // Prefer Student.name; fall back to User.name if null/blank
                String name = (s.getName() != null && !s.getName().isBlank())
                    ? s.getName()
                    : (s.getUser() != null ? s.getUser().getName() : "");
                String email = (s.getEmail() != null && !s.getEmail().isBlank())
                    ? s.getEmail()
                    : (s.getUser() != null ? s.getUser().getEmail() : "");
                String phone = (s.getPhone() != null && !s.getPhone().isBlank())
                    ? s.getPhone()
                    : (s.getUser() != null ? s.getUser().getPhone() : "");
                dto.put("name", name);
                dto.put("email", email);
                dto.put("phone", phone);
                dto.put("active", "Paid".equalsIgnoreCase(e.getPaymentStatus()));
                dto.put("joinedDate", e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "");
                // Online status
                if (s.getUser() != null) {
                    SecuritySettings ss = securitySettingsRepository.findByUserId(s.getUser().getId()).orElse(null);
                    boolean activityEnabled = ss == null || ss.isActivityStatusEnabled();
                    boolean isOnline = ss != null && ss.isOnline();
                    dto.put("onlineStatus", activityEnabled ? (isOnline ? "online" : "offline") : "always_online");
                } else {
                    dto.put("onlineStatus", "offline");
                }
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(students);
    }

    @PutMapping("/{id}/covered-topics")
    public ResponseEntity<Map<String, Object>> updateCoveredTopics(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Object raw = body.get("coveredTopics");
            if (raw == null) raw = body.get("topics");
            String topicsStr = "";
            if (raw instanceof List) {
                topicsStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(raw);
            } else if (raw != null) {
                topicsStr = raw.toString();
            }
            Batch updated = batchService.updateCoveredTopics(id, topicsStr);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "batchId", id,
                "coveredTopics", updated.getCoveredTopics() != null ? updated.getCoveredTopics() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/covered-topics")
    public ResponseEntity<Map<String, Object>> getCoveredTopics(@PathVariable Long id) {
        Batch batch = batchService.getBatchById(id);
        if (batch == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "success", true,
            "batchId", id,
            "coveredTopics", batch.getCoveredTopics() != null ? batch.getCoveredTopics() : ""
        ));
    }
}
