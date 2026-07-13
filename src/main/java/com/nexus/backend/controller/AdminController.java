package com.nexus.backend.controller;

import com.nexus.backend.dto.ApiResponse;
import com.nexus.backend.dto.CreateUserRequest;
import com.nexus.backend.model.Course;
import com.nexus.backend.model.Enrollment;
import com.nexus.backend.model.Student;
import com.nexus.backend.model.Teacher;
import com.nexus.backend.model.User;
import com.nexus.backend.repository.BatchRepository;
import com.nexus.backend.repository.CourseRepository;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getAdminProfile() {
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        User admin = admins.isEmpty() ? null : admins.get(0);

        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalCourses = courseRepository.count();
        BigDecimal revenue = courseRepository.findAll().stream()
            .filter(c -> c.getPrice() != null)
            .map(Course::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new HashMap<>();
        data.put("id", admin != null ? admin.getId() : null);
        data.put("name", admin != null ? admin.getName() : "Administrator");
        data.put("email", admin != null ? admin.getEmail() : "");
        data.put("createdAt", admin != null && admin.getCreatedAt() != null
            // amazonq-ignore-next-line
            ? admin.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")) : "");
        data.put("lastLogin", admin != null && admin.getLastLogin() != null
            // amazonq-ignore-next-line
            ? admin.getLastLogin().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a")) : "Not recorded");
        data.put("totalStudents", totalStudents);
        data.put("totalTeachers", totalTeachers);
        data.put("totalCourses", totalCourses);
        data.put("revenue", revenue);

        return ResponseEntity.ok(ApiResponse.ok("Admin profile fetched", data));
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    // amazonq-ignore-next-line
    public ResponseEntity<ApiResponse> getDashboard() {
        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalCourses = courseRepository.count();
        long activeCourses = courseRepository.findByStatus(Course.Status.ACTIVE).size();

        // Percentages based on real ratios
        String studentsPct = totalStudents > 0 ? "+" + Math.min(99, (int)((double) enrollmentRepository.count() / totalStudents * 100)) + "%" : "+0%";
        String teachersPct = totalTeachers > 0 ? "+" + Math.min(99, (int)((double) activeCourses / Math.max(1, totalCourses) * totalTeachers)) + "%" : "+0%";
        String coursesPct = totalCourses > 0 ? "+" + (int)((double) activeCourses / totalCourses * 100) + "%" : "+0%";

        BigDecimal revenue = courseRepository.findAll().stream()
            .filter(c -> c.getPrice() != null)
            .map(Course::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal activeRevenue = courseRepository.findByStatus(Course.Status.ACTIVE).stream()
            .filter(c -> c.getPrice() != null)
            .map(Course::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        String revenuePct = revenue.compareTo(BigDecimal.ZERO) > 0
            ? "+" + activeRevenue.multiply(new java.math.BigDecimal(100)).divide(revenue, 0, java.math.RoundingMode.HALF_UP) + "%"
            : "+0%";

        // Recent enrollments (last 10)
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();
        List<Map<String, Object>> recentEnrollments = allEnrollments.stream()
            .sorted((a, b) -> {
                String da = a.getEnrollmentDate() != null ? a.getEnrollmentDate() : "";
                String db = b.getEnrollmentDate() != null ? b.getEnrollmentDate() : "";
                return db.compareTo(da);
            })
            .limit(3)
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", e.getId());
                m.put("studentName", e.getStudent().getUser().getName());
                m.put("courseTitle", e.getCourseTitle());
                m.put("enrollmentDate", e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "");
                m.put("paymentStatus", e.getPaymentStatus() != null ? e.getPaymentStatus() : "");
                return m;
            }).collect(Collectors.toList());

        // Filter batches by today's day of week
        com.nexus.backend.enums.ClassDay todayDay = com.nexus.backend.enums.ClassDay.valueOf(
            java.time.LocalDate.now().getDayOfWeek().name().substring(0, 3));
        List<Map<String, Object>> classesToday = batchRepository.findAll().stream()
            .filter(b -> b.getStatus() == com.nexus.backend.enums.BatchStatus.ACTIVE
                && b.getClassDays() != null && b.getClassDays().contains(todayDay))
            .map(b -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", b.getId());
                m.put("course", b.getSelectCourse());
                m.put("instructor", b.getInstructor() != null ? b.getInstructor() : "");
                // Get class timings from the matching course
                String timings = courseRepository.findAll().stream()
                    .filter(c -> c.getTitle() != null && c.getTitle().equalsIgnoreCase(b.getSelectCourse())
                        && c.getClassTimings() != null && !c.getClassTimings().isBlank())
                    .map(Course::getClassTimings).findFirst().orElse("");
                m.put("time", timings);
                m.put("studentsCount", enrollmentRepository.countByCourseTitleIgnoreCase(b.getSelectCourse()));
                return m;
            }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("totalStudents", totalStudents);
        data.put("totalTeachers", totalTeachers);
        data.put("totalCourses", totalCourses);
        data.put("studentsPct", studentsPct);
        data.put("teachersPct", teachersPct);
        data.put("coursesPct", coursesPct);
        data.put("revenuePct", revenuePct);
        data.put("revenue", revenue);
        data.put("recentEnrollments", recentEnrollments);
        data.put("classesToday", classesToday);

        return ResponseEntity.ok(ApiResponse.ok("Dashboard data fetched", data));
    }

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
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getStudents() {
        List<Map<String, Object>> result = studentRepository.findAll().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("userId", s.getUser().getId());
            // Always prefer Student fields, fall back to User fields
            String name = (s.getName() != null && !s.getName().isBlank()) ? s.getName() : s.getUser().getName();
            String email = (s.getEmail() != null && !s.getEmail().isBlank()) ? s.getEmail() : s.getUser().getEmail();
            String phone = (s.getPhone() != null && !s.getPhone().isBlank()) ? s.getPhone() : (s.getUser().getPhone() != null ? s.getUser().getPhone() : "");
            m.put("name", name);
            m.put("email", email);
            m.put("phone", phone);
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
    @Transactional
    public ResponseEntity<ApiResponse> updateStudent(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        if (body.containsKey("firstName") && body.containsKey("lastName")) {
            String fullName = body.get("firstName") + " " + body.get("lastName");
            s.getUser().setName(fullName);
            s.setName(fullName); // sync Student.name so card reflects change
        }
        if (body.containsKey("phone")) { s.getUser().setPhone(body.get("phone")); s.setPhone(body.get("phone")); }
        if (body.containsKey("email")) { s.getUser().setEmail(body.get("email")); s.setEmail(body.get("email")); }
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

        // ── Also update the Enrollment record so the card reflects the change ──
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(s);
        if (!enrollments.isEmpty()) {
            // If a specific course is being updated, match it; otherwise update the first enrollment
            String targetCourse = body.containsKey("course") ? body.get("course") : null;
            Enrollment target = enrollments.stream()
                .filter(e -> targetCourse == null || e.getCourseTitle().equalsIgnoreCase(targetCourse))
                .findFirst()
                .orElse(enrollments.get(0));
            if (body.containsKey("paymentStatus"))
                target.setPaymentStatus(body.get("paymentStatus"));
            if (body.containsKey("enrollmentDate"))
                target.setEnrollmentDate(body.get("enrollmentDate"));
            enrollmentRepository.save(target);
        }

        return ResponseEntity.ok(ApiResponse.ok("Student updated", null));
    }

    @PostMapping("/students/{id}/enroll")
    @Transactional
    public ResponseEntity<ApiResponse> enrollStudentInCourse(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        String courseTitle = body.get("courseTitle");
        if (courseTitle == null || courseTitle.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("courseTitle is required"));
        if (enrollmentRepository.existsByStudentAndCourseTitle(s, courseTitle))
            return ResponseEntity.badRequest().body(ApiResponse.error("Student is already enrolled in this course"));
        Enrollment e = new Enrollment();
        e.setStudent(s);
        e.setCourseTitle(courseTitle);
        e.setEnrollmentDate(body.getOrDefault("enrollmentDate", java.time.LocalDate.now().toString()));
        e.setPaymentStatus(body.getOrDefault("paymentStatus", "Pending"));
        enrollmentRepository.save(e);
        return ResponseEntity.ok(ApiResponse.ok("Student enrolled in " + courseTitle, null));
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
    // amazonq-ignore-next-line
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
    // amazonq-ignore-next-line
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
        // amazonq-ignore-next-line
        // amazonq-ignore-next-line
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed: " + errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Unexpected error: " + ex.getMessage()));
    }
}
