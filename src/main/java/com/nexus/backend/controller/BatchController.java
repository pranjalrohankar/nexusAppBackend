package com.nexus.backend.controller;

import com.nexus.backend.dto.BatchDto;
import com.nexus.backend.model.Batch;
import com.nexus.backend.model.Enrollment;
import com.nexus.backend.model.Student;
import com.nexus.backend.repository.EnrollmentRepository;
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

    /**
     * Returns students enrolled in the course of the given batch.
     * Uses @Transactional to keep the Hibernate session open while
     * accessing lazily-loaded Student associations on Enrollment.
     */
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
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(students);
    }
}
