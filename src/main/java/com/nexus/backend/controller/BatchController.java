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

    private String extractCoveredTopics(Object body) {
        if (body == null) return "";
        try {
            if (body instanceof String str) {
                String trimmed = str.trim();
                if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
                    try {
                        com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(trimmed);
                        if (node.isArray()) {
                            return trimmed;
                        } else if (node.has("coveredTopics")) {
                            com.fasterxml.jackson.databind.JsonNode ct = node.get("coveredTopics");
                            return ct.isTextual() ? ct.asText() : ct.toString();
                        } else if (node.has("topics")) {
                            com.fasterxml.jackson.databind.JsonNode ct = node.get("topics");
                            return ct.isTextual() ? ct.asText() : ct.toString();
                        }
                    } catch (Exception ignored) {}
                }
                return trimmed;
            } else if (body instanceof List<?> list) {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
            } else if (body instanceof Map<?, ?> map) {
                Object raw = map.get("coveredTopics");
                if (raw == null) raw = map.get("topics");
                if (raw == null) raw = map.get("data");
                if (raw instanceof List) {
                    return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(raw);
                } else if (raw != null) {
                    return raw.toString();
                }
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
            }
        } catch (Exception ignored) {}
        return body.toString();
    }

    @PutMapping("/{id}/covered-topics")
    public ResponseEntity<Map<String, Object>> updateCoveredTopics(
            @PathVariable Long id,
            @RequestBody(required = false) Object body) {
        String topicsStr = extractCoveredTopics(body);
        try {
            Batch updated = batchService.updateCoveredTopics(id, topicsStr);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "batchId", id != null ? id : 0L,
                "coveredTopics", updated != null && updated.getCoveredTopics() != null ? updated.getCoveredTopics() : topicsStr
            ));
        } catch (Throwable t) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "batchId", id != null ? id : 0L,
                "coveredTopics", topicsStr
            ));
        }
    }

    @GetMapping("/{id}/covered-topics")
    public ResponseEntity<Map<String, Object>> getCoveredTopics(@PathVariable Long id) {
        try {
            Batch batch = batchService.getBatchById(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "batchId", id,
                "coveredTopics", batch != null && batch.getCoveredTopics() != null ? batch.getCoveredTopics() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", true, "batchId", id, "coveredTopics", ""));
        }
    }
}
