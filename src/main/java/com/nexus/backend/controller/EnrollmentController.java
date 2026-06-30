package com.nexus.backend.controller;

import com.nexus.backend.model.Enrollment;
import com.nexus.backend.model.Student;
import com.nexus.backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;

    @GetMapping("/course")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEnrollmentsByCourse(@RequestParam String courseTitle) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourseTitleIgnoreCase(courseTitle);
        return enrollments.stream().map(e -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", e.getId());
            dto.put("courseTitle", e.getCourseTitle());
            dto.put("enrollmentDate", e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "");
            dto.put("paymentStatus", e.getPaymentStatus() != null ? e.getPaymentStatus() : "");
            Student s = e.getStudent();
            if (s != null) {
                dto.put("studentId", s.getId());
                String name = (s.getName() != null && !s.getName().isBlank())
                    ? s.getName()
                    : (s.getUser() != null ? s.getUser().getName() : "");
                String email = (s.getEmail() != null && !s.getEmail().isBlank())
                    ? s.getEmail()
                    : (s.getUser() != null ? s.getUser().getEmail() : "");
                String phone = (s.getPhone() != null && !s.getPhone().isBlank())
                    ? s.getPhone()
                    : (s.getUser() != null ? s.getUser().getPhone() : "");
                dto.put("studentName", name);
                dto.put("name", name);
                dto.put("email", email);
                dto.put("phone", phone);
                dto.put("active", "Paid".equalsIgnoreCase(e.getPaymentStatus()));
                dto.put("joinedDate", e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "");
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @GetMapping("/count/course")
    public Map<String, Integer> getEnrollmentCount(@RequestParam String courseTitle) {
        return Map.of("count", enrollmentRepository.countByCourseTitleIgnoreCase(courseTitle));
    }
}
