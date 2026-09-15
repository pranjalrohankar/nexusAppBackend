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

    private final com.nexus.backend.repository.StudentRepository studentRepository;
    private final com.nexus.backend.repository.BatchRepository batchRepository;

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

        boolean isBatchActive = (batch.getEffectiveStatus() != com.nexus.backend.enums.BatchStatus.COMPLETED);

        // Fetch enrollments and filter for this batch specifically
        List<Enrollment> courseEnrollments = enrollmentRepository.findByCourseTitleIgnoreCase(batch.getSelectCourse());

        List<Enrollment> enrollments = courseEnrollments.stream().filter(e -> {
            if (e.getBatchId() != null && e.getBatchId().equals(batch.getId())) return true;
            if (e.getBatchName() != null && !e.getBatchName().isBlank() && e.getBatchName().equalsIgnoreCase(batch.getBatchName())) return true;
            // Legacy student enrolled in course without specific batch assigned
            if ((e.getBatchName() == null || e.getBatchName().isBlank()) && e.getBatchId() == null) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());

        List<Map<String, Object>> students = enrollments.stream().map(e -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("enrollmentId", e.getId());
            dto.put("courseTitle", e.getCourseTitle());
            dto.put("batchName", e.getBatchName() != null ? e.getBatchName() : batch.getBatchName());
            dto.put("batchId", batch.getId());
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
                dto.put("active", isBatchActive);
                dto.put("status", isBatchActive ? "Active" : "Inactive");
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

    @PutMapping("/{id}/reassign-student")
    @Transactional
    public ResponseEntity<Map<String, Object>> reassignStudentFromBatch(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long studentId = body.get("studentId") != null && !body.get("studentId").toString().isBlank()
            ? Long.valueOf(body.get("studentId").toString()) : null;
        Long targetBatchId = body.get("targetBatchId") != null && !body.get("targetBatchId").toString().isBlank()
            ? Long.valueOf(body.get("targetBatchId").toString()) : null;
        String targetBatchName = body.get("targetBatchName") != null ? body.get("targetBatchName").toString().trim() : null;

        if (studentId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "studentId is required"));
        }

        Batch currentBatch = batchService.getBatchById(id);
        Batch targetBatch = targetBatchId != null ? batchService.getBatchById(targetBatchId) : null;
        if (targetBatch == null && targetBatchName != null && !targetBatchName.isBlank()) {
            targetBatch = batchRepository.findAll().stream()
                    .filter(b -> b.getBatchName() != null && b.getBatchName().equalsIgnoreCase(targetBatchName.trim()))
                    .findFirst().orElse(null);
        }

        Student s = studentRepository.findById(studentId).orElse(null);
        if (s == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student not found"));
        }

        List<Enrollment> enrollments = enrollmentRepository.findByStudent(s);
        String courseToMatch = currentBatch != null ? currentBatch.getSelectCourse() : null;
        Enrollment enrollment = enrollments.stream()
                .filter(e -> (courseToMatch != null && e.getCourseTitle() != null && e.getCourseTitle().equalsIgnoreCase(courseToMatch)) ||
                        (e.getBatchId() != null && e.getBatchId().equals(id)) ||
                        (currentBatch != null && e.getBatchName() != null && e.getBatchName().equalsIgnoreCase(currentBatch.getBatchName())))
                .findFirst()
                .orElse(!enrollments.isEmpty() ? enrollments.get(0) : null);

        if (enrollment == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No matching enrollment found for student"));
        }

        if (targetBatch != null) {
            enrollment.setBatchId(targetBatch.getId());
            enrollment.setBatchName(targetBatch.getBatchName());
            if (targetBatch.getSelectCourse() != null && !targetBatch.getSelectCourse().isBlank()) {
                enrollment.setCourseTitle(targetBatch.getSelectCourse());
            }
        } else if (targetBatchName != null && !targetBatchName.isBlank()) {
            enrollment.setBatchName(targetBatchName.trim());
            enrollment.setBatchId(null);
        } else {
            enrollment.setBatchName(null);
            enrollment.setBatchId(null);
        }

        enrollmentRepository.save(enrollment);
        return ResponseEntity.ok(Map.of("success", true, "message", "Student reassigned to batch successfully"));
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
