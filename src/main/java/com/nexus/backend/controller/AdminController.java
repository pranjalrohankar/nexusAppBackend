package com.nexus.backend.controller;

import com.nexus.backend.dto.ApiResponse;
import com.nexus.backend.dto.CreateUserRequest;
import com.nexus.backend.model.Student;
import com.nexus.backend.model.Teacher;
import com.nexus.backend.repository.EnrollmentRepository;
import com.nexus.backend.repository.StudentRepository;
import com.nexus.backend.repository.TeacherRepository;
import com.nexus.backend.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

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
            m.put("dob", s.getDob() != null ? s.getDob() : "");
            m.put("street", s.getStreet() != null ? s.getStreet() : "");
            m.put("state", s.getState() != null ? s.getState() : "");
            m.put("pinCode", s.getPinCode() != null ? s.getPinCode() : "");
            m.put("guardianPhone", s.getGuardianPhone() != null ? s.getGuardianPhone() : "");
            m.put("createdAt", s.getUser().getCreatedAt() != null ? s.getUser().getCreatedAt().toString() : "");
            List<Map<String, String>> enrollments = enrollmentRepository.findByStudent(s).stream().map(e -> {
                Map<String, String> em = new HashMap<>();
                em.put("courseTitle", e.getCourseTitle());
                em.put("enrollmentDate", e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "");
                em.put("paymentStatus", e.getPaymentStatus() != null ? e.getPaymentStatus() : "");
                return em;
            }).collect(Collectors.toList());
            m.put("enrollments", enrollments);
            m.put("coursesCount", enrollments.size());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Students fetched", result));
    }

    @PutMapping("/students/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<ApiResponse> updateStudent(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        if (body.containsKey("firstName") && body.containsKey("lastName"))
            s.getUser().setName(body.get("firstName") + " " + body.get("lastName"));
        if (body.containsKey("phone")) s.getUser().setPhone(body.get("phone"));
        if (body.containsKey("dob")) s.setDob(body.get("dob"));
        if (body.containsKey("street")) s.setStreet(body.get("street"));
        if (body.containsKey("city")) s.setCity(body.get("city"));
        if (body.containsKey("state")) s.setState(body.get("state"));
        if (body.containsKey("pinCode")) s.setPinCode(body.get("pinCode"));
        if (body.containsKey("guardianName")) s.setGuardianName(body.get("guardianName"));
        if (body.containsKey("guardianPhone")) s.setGuardianPhone(body.get("guardianPhone"));
        if (body.containsKey("course")) s.setCourse(body.get("course"));
        if (body.containsKey("enrollmentDate")) s.setEnrollmentDate(body.get("enrollmentDate"));
        if (body.containsKey("paymentStatus")) s.setPaymentStatus(body.get("paymentStatus"));
        userRepository.save(s.getUser());
        studentRepository.save(s);
        return ResponseEntity.ok(ApiResponse.ok("Student updated", null));
    }

    @DeleteMapping("/students/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<ApiResponse> deleteStudent(@PathVariable Long id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        var user = s.getUser();
        enrollmentRepository.deleteLegacyEnrollmentsByStudentId(s.getId());
        enrollmentRepository.deleteByStudent(s);
        studentRepository.delete(s);
        userRepository.delete(user);
        return ResponseEntity.ok(ApiResponse.ok("Student deleted", null));
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
            m.put("dob", t.getDob() != null ? t.getDob() : "");
            m.put("street", t.getStreet() != null ? t.getStreet() : "");
            m.put("city", t.getCity() != null ? t.getCity() : "");
            m.put("state", t.getState() != null ? t.getState() : "");
            m.put("pinCode", t.getPinCode() != null ? t.getPinCode() : "");
            m.put("createdAt", t.getUser().getCreatedAt() != null ? t.getUser().getCreatedAt().toString() : "");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Teachers fetched", result));
    }

    @PutMapping("/teachers/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<ApiResponse> updateTeacher(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Teacher t = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        if (body.containsKey("firstName") && body.containsKey("lastName"))
            t.getUser().setName(body.get("firstName") + " " + body.get("lastName"));
        if (body.containsKey("phone")) t.getUser().setPhone(body.get("phone"));
        if (body.containsKey("dob")) t.setDob(body.get("dob"));
        if (body.containsKey("street")) t.setStreet(body.get("street"));
        if (body.containsKey("city")) t.setCity(body.get("city"));
        if (body.containsKey("state")) t.setState(body.get("state"));
        if (body.containsKey("pinCode")) t.setPinCode(body.get("pinCode"));
        if (body.containsKey("qualification")) t.setQualification(body.get("qualification"));
        if (body.containsKey("experience")) t.setExperience(body.get("experience"));
        if (body.containsKey("specialization")) t.setSpecialization(body.get("specialization"));
        if (body.containsKey("joinDate")) t.setJoinDate(body.get("joinDate"));
        if (body.containsKey("employmentType")) t.setEmploymentType(body.get("employmentType"));
        userRepository.save(t.getUser());
        teacherRepository.save(t);
        return ResponseEntity.ok(ApiResponse.ok("Teacher updated", null));
    }

    @DeleteMapping("/teachers/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<ApiResponse> deleteTeacher(@PathVariable Long id) {
        Teacher t = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        var user = t.getUser();
        teacherRepository.delete(t);
        userRepository.delete(user);
        return ResponseEntity.ok(ApiResponse.ok("Teacher deleted", null));
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
