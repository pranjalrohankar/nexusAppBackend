package com.nexus.backend.controller;

import com.nexus.backend.dto.ApiResponse;
import com.nexus.backend.dto.CreateUserRequest;
import com.nexus.backend.model.Student;
import com.nexus.backend.model.Teacher;
import com.nexus.backend.repository.StudentRepository;
import com.nexus.backend.repository.TeacherRepository;
import com.nexus.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @PostMapping("/users")
    public ResponseEntity<ApiResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            boolean isNew = !userService.userExists(request.getEmail());
            var user = userService.createUser(request);
            String msg = isNew
                    ? request.getRole() + " registered and credentials emailed successfully"
                    : "Existing student enrolled in new course and notified by email";
            return ResponseEntity.ok(ApiResponse.ok(msg, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse> getStudents() {
        List<Map<String, Object>> result = studentRepository.findAll().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("userId", s.getUser().getId());
            m.put("name", s.getUser().getName());
            m.put("email", s.getUser().getEmail());
            m.put("phone", s.getUser().getPhone() != null ? s.getUser().getPhone() : "");
            m.put("active", s.getUser().isActive());
            m.put("course", s.getCourse() != null ? s.getCourse() : "");
            m.put("enrollmentDate", s.getEnrollmentDate() != null ? s.getEnrollmentDate() : "");
            m.put("paymentStatus", s.getPaymentStatus() != null ? s.getPaymentStatus() : "");
            m.put("city", s.getCity() != null ? s.getCity() : "");
            m.put("guardianName", s.getGuardianName() != null ? s.getGuardianName() : "");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Students fetched", result));
    }

    @GetMapping("/teachers")
    public ResponseEntity<ApiResponse> getTeachers() {
        List<Map<String, Object>> result = teacherRepository.findAll().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("userId", t.getUser().getId());
            m.put("name", t.getUser().getName());
            m.put("email", t.getUser().getEmail());
            m.put("phone", t.getUser().getPhone() != null ? t.getUser().getPhone() : "");
            m.put("active", t.getUser().isActive());
            m.put("specialization", t.getSpecialization() != null ? t.getSpecialization() : "");
            m.put("qualification", t.getQualification() != null ? t.getQualification() : "");
            m.put("experience", t.getExperience() != null ? t.getExperience() : "");
            m.put("employmentType", t.getEmploymentType() != null ? t.getEmploymentType() : "");
            m.put("joinDate", t.getJoinDate() != null ? t.getJoinDate() : "");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Teachers fetched", result));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed: " + errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Unexpected error: " + ex.getMessage()));
    }
}
