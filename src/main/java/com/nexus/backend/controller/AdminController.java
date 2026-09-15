package com.nexus.backend.controller;

import com.nexus.backend.dto.ApiResponse;
import com.nexus.backend.dto.CreateUserRequest;
import com.nexus.backend.model.Course;
import com.nexus.backend.model.Enrollment;
import com.nexus.backend.model.SecuritySettings;
import com.nexus.backend.model.Student;
import com.nexus.backend.model.Teacher;
import com.nexus.backend.model.User;
import com.nexus.backend.repository.BatchRepository;
import com.nexus.backend.repository.CourseRepository;
import com.nexus.backend.repository.EnrollmentRepository;
import com.nexus.backend.repository.StudentRepository;
import com.nexus.backend.repository.TeacherRepository;
import com.nexus.backend.repository.TeacherCourseAssignmentRepository;
import com.nexus.backend.repository.UserRepository;
import com.nexus.backend.repository.SecuritySettingsRepository;
import com.nexus.backend.service.UserService;
import com.nexus.backend.service.AppNotificationService;
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
    private final TeacherCourseAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final SecuritySettingsRepository securitySettingsRepository;
    private final AppNotificationService appNotificationService;
    private final com.nexus.backend.repository.PasswordResetRequestRepository passwordResetRequestRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getAdminProfile() {
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        User admin = admins.isEmpty() ? null : admins.get(0);

        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalCourses = courseRepository.count();

        // Build price map of courseTitle -> course price
        Map<String, BigDecimal> coursePriceMap = courseRepository.findAll().stream()
            .filter(c -> c.getTitle() != null && c.getPrice() != null)
            .collect(Collectors.toMap(
                c -> c.getTitle().trim().toLowerCase(),
                Course::getPrice,
                (existing, replacement) -> existing
            ));

        // Revenue generated strictly from Paid student enrollments
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();
        BigDecimal revenue = allEnrollments.stream()
            .filter(e -> e.getPaymentStatus() != null && "paid".equalsIgnoreCase(e.getPaymentStatus().trim()))
            .map(e -> {
                String titleKey = e.getCourseTitle() != null ? e.getCourseTitle().trim().toLowerCase() : "";
                BigDecimal price = coursePriceMap.get(titleKey);
                return price != null ? price : new BigDecimal("5000");
            })
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

        List<Enrollment> allEnrollments = enrollmentRepository.findAll();
        long paidEnrollmentsCount = allEnrollments.stream()
            .filter(e -> e.getPaymentStatus() != null && "paid".equalsIgnoreCase(e.getPaymentStatus().trim()))
            .count();

        // Percentages based on real ratios
        String studentsPct = totalStudents > 0 ? "+" + Math.min(99, (int)((double) enrollmentRepository.count() / totalStudents * 100)) + "%" : "+0%";
        String teachersPct = totalTeachers > 0 ? "+" + Math.min(99, (int)((double) activeCourses / Math.max(1, totalCourses) * totalTeachers)) + "%" : "+0%";
        String coursesPct = totalCourses > 0 ? "+" + (int)((double) activeCourses / totalCourses * 100) + "%" : "+0%";

        // Build price map of courseTitle -> course price
        Map<String, BigDecimal> coursePriceMap = courseRepository.findAll().stream()
            .filter(c -> c.getTitle() != null && c.getPrice() != null)
            .collect(Collectors.toMap(
                c -> c.getTitle().trim().toLowerCase(),
                Course::getPrice,
                (existing, replacement) -> existing
            ));

        // Revenue generated strictly from Paid student enrollments
        BigDecimal revenue = allEnrollments.stream()
            .filter(e -> e.getPaymentStatus() != null && "paid".equalsIgnoreCase(e.getPaymentStatus().trim()))
            .map(e -> {
                String titleKey = e.getCourseTitle() != null ? e.getCourseTitle().trim().toLowerCase() : "";
                BigDecimal price = coursePriceMap.get(titleKey);
                return price != null ? price : new BigDecimal("5000");
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String revenuePct = allEnrollments.size() > 0
            ? "+" + (int)((double) paidEnrollmentsCount / allEnrollments.size() * 100) + "%"
            : "+0%";

        // Recent enrollments (last 10)
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
                // Online status
                Student st = e.getStudent();
                if (st != null && st.getUser() != null) {
                    SecuritySettings ss = securitySettingsRepository.findByUserId(st.getUser().getId()).orElse(null);
                    boolean activityEnabled = ss == null || ss.isActivityStatusEnabled();
                    boolean isOnline = ss != null && ss.isOnline();
                    m.put("onlineStatus", activityEnabled ? (isOnline ? "online" : "offline") : "always_online");
                } else {
                    m.put("onlineStatus", "offline");
                }
                return m;
            }).collect(Collectors.toList());

        // Filter batches by today's day of week
        com.nexus.backend.enums.ClassDay todayDay = com.nexus.backend.enums.ClassDay.valueOf(
            java.time.LocalDate.now().getDayOfWeek().name().substring(0, 3));
        List<Map<String, Object>> classesToday = batchRepository.findAll().stream()
            .filter(b -> b.getEffectiveStatus() == com.nexus.backend.enums.BatchStatus.ACTIVE
                && b.getClassDays() != null && b.getClassDays().contains(todayDay))
            .map(b -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", b.getId());
                m.put("course", b.getSelectCourse());
                m.put("instructor", b.getInstructor() != null ? b.getInstructor() : "");
                // Get class timings from batch first, then fallback to course
                String timings = (b.getClassTimings() != null && !b.getClassTimings().isBlank())
                    ? b.getClassTimings()
                    : courseRepository.findAll().stream()
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
            // Notify teacher when a student is added/enrolled with a course
            if ("STUDENT".equalsIgnoreCase(request.getRole())
                    && request.getCourse() != null && !request.getCourse().isBlank()) {
                String studentName = request.getFirstName() + " " + request.getLastName();
                String courseTitle = request.getCourse();
                String teacherEmail = batchRepository.findAll().stream()
                    .filter(b -> b.getSelectCourse() != null
                        && b.getSelectCourse().equalsIgnoreCase(courseTitle)
                        && b.getInstructor() != null && !b.getInstructor().isBlank())
                    .findFirst()
                    .map(b -> userRepository.findAll().stream()
                        .filter(u -> u.getName() != null && u.getName().equalsIgnoreCase(b.getInstructor()))
                        .map(u -> u.getEmail()).findFirst().orElse(null))
                    .orElse(null);
                if (teacherEmail != null) {
                    appNotificationService.notifyEnrollment(studentName, courseTitle, teacherEmail);
                } else {
                    System.out.println(">>> [NOTIFY DEBUG] No teacher email found for course: '" + courseTitle + "'. Check batch instructor name matches user name exactly.");
                    batchRepository.findAll().stream()
                        .filter(b -> b.getSelectCourse() != null && b.getSelectCourse().equalsIgnoreCase(courseTitle))
                        .forEach(b -> System.out.println(">>> [NOTIFY DEBUG] Batch found: '" + b.getBatchName() + "', instructor: '" + b.getInstructor() + "'"));
                }
            }
            return ResponseEntity.ok(ApiResponse.ok(msg, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/students")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getStudents() {
        List<com.nexus.backend.model.Batch> allBatches = batchRepository.findAll();

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

            // Collect all unique course titles and explicit batches the student is enrolled in
            List<Enrollment> rawEnrollments = enrollmentRepository.findByStudent(s);
            if (rawEnrollments.isEmpty() && s.getCourse() != null && !s.getCourse().isBlank()) {
                Enrollment autoEnr = new Enrollment();
                autoEnr.setStudent(s);
                autoEnr.setCourseTitle(s.getCourse().trim());
                autoEnr.setEnrollmentDate(s.getEnrollmentDate() != null ? s.getEnrollmentDate() : "2026-06-01");
                autoEnr.setPaymentStatus(s.getPaymentStatus() != null ? s.getPaymentStatus() : "PAID");
                try {
                    autoEnr = enrollmentRepository.save(autoEnr);
                    rawEnrollments = List.of(autoEnr);
                } catch (Exception ignored) {}
            }

            // Deduplicate enrollments by courseTitle (case-insensitive)
            Map<String, Enrollment> uniqueEnrollmentsMap = new LinkedHashMap<>();
            List<Enrollment> duplicatesToDelete = new ArrayList<>();
            for (Enrollment e : rawEnrollments) {
                if (e.getCourseTitle() == null || e.getCourseTitle().isBlank()) continue;
                String cKey = e.getCourseTitle().trim().toLowerCase();
                if (!uniqueEnrollmentsMap.containsKey(cKey)) {
                    uniqueEnrollmentsMap.put(cKey, e);
                } else {
                    Enrollment existing = uniqueEnrollmentsMap.get(cKey);
                    // If existing has no batch but this duplicate has a batch, keep the one with the batch
                    if ((existing.getBatchName() == null || existing.getBatchName().isBlank()) && (e.getBatchName() != null && !e.getBatchName().isBlank())) {
                        duplicatesToDelete.add(existing);
                        uniqueEnrollmentsMap.put(cKey, e);
                    } else {
                        duplicatesToDelete.add(e);
                    }
                }
            }
            if (!duplicatesToDelete.isEmpty()) {
                try {
                    enrollmentRepository.deleteAll(duplicatesToDelete);
                } catch (Exception ignored) {}
            }
            List<Enrollment> studentEnrollments = new ArrayList<>(uniqueEnrollmentsMap.values());

            java.util.Set<String> enrolledCourseTitles = new java.util.HashSet<>();
            if (s.getCourse() != null && !s.getCourse().isBlank()) {
                enrolledCourseTitles.add(s.getCourse().trim().toLowerCase());
            }
            for (Enrollment e : studentEnrollments) {
                if (e.getCourseTitle() != null && !e.getCourseTitle().isBlank()) {
                    enrolledCourseTitles.add(e.getCourseTitle().trim().toLowerCase());
                }
            }

            // Find matching batches ONLY from explicit assignments (batchId or batchName)
            List<com.nexus.backend.model.Batch> studentBatches = new java.util.ArrayList<>();
            java.util.Set<Long> addedBatchIds = new java.util.HashSet<>();

            for (Enrollment e : studentEnrollments) {
                com.nexus.backend.model.Batch matched = null;
                if (e.getBatchId() != null) {
                    matched = allBatches.stream().filter(b -> e.getBatchId().equals(b.getId())).findFirst().orElse(null);
                }
                if (matched == null && e.getBatchName() != null && !e.getBatchName().isBlank() && !e.getBatchName().equalsIgnoreCase("No Batch")) {
                    matched = allBatches.stream().filter(b -> b.getBatchName() != null && b.getBatchName().equalsIgnoreCase(e.getBatchName().trim())).findFirst().orElse(null);
                }
                if (matched != null && addedBatchIds.add(matched.getId())) {
                    studentBatches.add(matched);
                }
            }

            // Determine active status:
            // If student has matching batches:
            //   - if all matching batches are COMPLETED, active = false (Inactive)
            //   - if any matching batch is ACTIVE or UPCOMING, active = true (Active)
            // If student has enrollments (even with No Batch): active = true
            // If student has no courses enrolled: active = false (Inactive)
            boolean isStudentActive = true;
            if (!studentBatches.isEmpty()) {
                boolean allCompleted = studentBatches.stream()
                    .allMatch(b -> b.getEffectiveStatus() == com.nexus.backend.enums.BatchStatus.COMPLETED);
                isStudentActive = !allCompleted;
            } else if (enrolledCourseTitles.isEmpty()) {
                isStudentActive = false;
            }

            m.put("active", isStudentActive);
            m.put("status", isStudentActive ? "Active" : "Inactive");

            List<Map<String, Object>> batchList = studentBatches.stream().map(b -> {
                Map<String, Object> bm = new HashMap<>();
                bm.put("id", b.getId());
                bm.put("batchName", b.getBatchName());
                bm.put("selectCourse", b.getSelectCourse());
                bm.put("status", b.getEffectiveStatus() != null ? b.getEffectiveStatus().name() : "ACTIVE");
                bm.put("startDate", b.getStartDate() != null ? b.getStartDate().toString() : "");
                bm.put("endDate", b.getEndDate() != null ? b.getEndDate().toString() : "");
                return bm;
            }).collect(Collectors.toList());
            m.put("batches", batchList);

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
            List<Map<String, Object>> enrollments = studentEnrollments.stream().map(e -> {
                Map<String, Object> em = new HashMap<>();
                em.put("id", e.getId());
                em.put("courseTitle", e.getCourseTitle());
                em.put("enrollmentDate", e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "");
                em.put("paymentStatus", e.getPaymentStatus() != null ? e.getPaymentStatus() : "");
                em.put("batchName", e.getBatchName() != null ? e.getBatchName() : "");
                em.put("batchId", e.getBatchId());
                return em;
            }).collect(Collectors.toList());
            m.put("enrollments", enrollments);
            m.put("coursesCount", enrollments.size());
            // Activity status: if activityStatusEnabled=false → always show as online; else use real online flag
            com.nexus.backend.model.SecuritySettings ss = securitySettingsRepository.findByUserId(s.getUser().getId()).orElse(null);
            boolean activityEnabled = ss == null || ss.isActivityStatusEnabled();
            boolean isOnline = ss != null && ss.isOnline();
            m.put("onlineStatus", activityEnabled ? (isOnline ? "online" : "offline") : "always_online");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Students fetched", result));
    }

    @PutMapping("/students/{id}")
    @SuppressWarnings("null")
    @Transactional
    public ResponseEntity<ApiResponse> updateStudent(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        if (body.containsKey("firstName") && body.containsKey("lastName")) {
            String fullName = body.get("firstName") + " " + body.get("lastName");
            s.getUser().setName(fullName);
            s.setName(fullName); // sync Student.name so card reflects change
        }
        if (body.containsKey("phone")) {
            String phone = body.get("phone") != null ? body.get("phone").toString().trim() : "";
            if (!phone.isBlank() && !phone.matches("^[6-9]\\d{9}$")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid mobile number. Must be a 10-digit number starting with 6, 7, 8, or 9."));
            }
            s.getUser().setPhone(phone);
            s.setPhone(phone);
        }
        if (body.containsKey("email")) { String em = body.get("email").toString(); s.getUser().setEmail(em); s.setEmail(em); }
        if (body.containsKey("dob")) s.setDob((String) body.get("dob"));
        if (body.containsKey("street")) s.setStreet((String) body.get("street"));
        if (body.containsKey("city")) s.setCity((String) body.get("city"));
        if (body.containsKey("state")) s.setState((String) body.get("state"));
        if (body.containsKey("pinCode")) s.setPinCode((String) body.get("pinCode"));
        if (body.containsKey("guardianName")) s.setGuardianName((String) body.get("guardianName"));
        if (body.containsKey("guardianPhone")) {
            String gPhone = body.get("guardianPhone") != null ? body.get("guardianPhone").toString().trim() : "";
            if (!gPhone.isBlank() && !gPhone.matches("^[6-9]\\d{9}$")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid guardian mobile number. Must be a 10-digit number starting with 6, 7, 8, or 9."));
            }
            s.setGuardianPhone(gPhone);
        }
        if (body.containsKey("course")) s.setCourse((String) body.get("course"));
        if (body.containsKey("enrollmentDate")) s.setEnrollmentDate((String) body.get("enrollmentDate"));
        if (body.containsKey("paymentStatus")) s.setPaymentStatus((String) body.get("paymentStatus"));
        userRepository.save(s.getUser());
        studentRepository.save(s);

        // ── Update Enrollment records and Batch assignments ──
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(s);
        List<com.nexus.backend.model.Batch> allBatches = batchRepository.findAll();

        // 1. Handle courseBatches map (courseTitle -> batchName)
        Map<String, String> courseBatches = new HashMap<>();
        if (body.containsKey("courseBatches")) {
            Object cbObj = body.get("courseBatches");
            if (cbObj instanceof Map) {
                ((Map<?, ?>) cbObj).forEach((k, v) -> {
                    if (k != null) courseBatches.put(k.toString().trim(), v != null ? v.toString().trim() : "");
                });
            } else if (cbObj instanceof String str && !str.isBlank()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, String> parsed = mapper.readValue(str, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                    courseBatches.putAll(parsed);
                } catch (Exception ignored) {}
            }
        }

        // 2. Single batchName fallback
        String singleBatchName = body.containsKey("batchName") && body.get("batchName") != null ? body.get("batchName").toString().trim() : null;

        // If student has no enrollments in DB yet, create one from s.getCourse() or body.course
        if (enrollments.isEmpty()) {
            String courseTitle = body.containsKey("course") && body.get("course") != null && !body.get("course").toString().isBlank()
                ? body.get("course").toString().trim()
                : (s.getCourse() != null && !s.getCourse().isBlank() ? s.getCourse().trim() : null);

            if (courseTitle == null && !courseBatches.isEmpty()) {
                courseTitle = courseBatches.keySet().iterator().next();
            }

            if (courseTitle != null && !courseTitle.isBlank()) {
                Enrollment newEnr = new Enrollment();
                newEnr.setStudent(s);
                newEnr.setCourseTitle(courseTitle);
                newEnr.setEnrollmentDate(s.getEnrollmentDate() != null ? s.getEnrollmentDate() : "2026-06-01");
                newEnr.setPaymentStatus(s.getPaymentStatus() != null ? s.getPaymentStatus() : "PAID");
                newEnr = enrollmentRepository.save(newEnr);
                enrollments.add(newEnr);
            }
        }

        // Also check if any key in courseBatches doesn't have an enrollment yet
        for (Map.Entry<String, String> entry : courseBatches.entrySet()) {
            String cTitle = entry.getKey();
            if (cTitle == null || cTitle.isBlank()) continue;
            boolean exists = enrollments.stream().anyMatch(e -> e.getCourseTitle() != null && e.getCourseTitle().equalsIgnoreCase(cTitle.trim()));
            if (!exists) {
                Enrollment extraEnr = new Enrollment();
                extraEnr.setStudent(s);
                extraEnr.setCourseTitle(cTitle.trim());
                extraEnr.setEnrollmentDate(s.getEnrollmentDate() != null ? s.getEnrollmentDate() : "2026-06-01");
                extraEnr.setPaymentStatus(s.getPaymentStatus() != null ? s.getPaymentStatus() : "PAID");
                extraEnr = enrollmentRepository.save(extraEnr);
                enrollments.add(extraEnr);
            }
        }

        for (Enrollment enr : enrollments) {
            String cTitle = enr.getCourseTitle() != null ? enr.getCourseTitle().trim() : "";
            String newBatchForCourse = courseBatches.get(cTitle);
            if (newBatchForCourse == null) {
                // Try case-insensitive lookup
                for (Map.Entry<String, String> entry : courseBatches.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(cTitle)) {
                        newBatchForCourse = entry.getValue();
                        break;
                    }
                }
            }

            if (newBatchForCourse != null) {
                if (newBatchForCourse.isBlank() || newBatchForCourse.equalsIgnoreCase("No Batch") || newBatchForCourse.contains("No Batch")) {
                    enr.setBatchName(null);
                    enr.setBatchId(null);
                } else {
                    String finalName = newBatchForCourse.trim();
                    com.nexus.backend.model.Batch b = allBatches.stream()
                        .filter(x -> x.getBatchName() != null && x.getBatchName().equalsIgnoreCase(finalName))
                        .findFirst().orElse(null);
                    enr.setBatchName(finalName);
                    enr.setBatchId(b != null ? b.getId() : null);
                }
            } else if (singleBatchName != null && enrollments.size() == 1) {
                if (singleBatchName.isBlank() || singleBatchName.equalsIgnoreCase("No Batch") || singleBatchName.contains("No Batch")) {
                    enr.setBatchName(null);
                    enr.setBatchId(null);
                } else {
                    String finalName = singleBatchName.trim();
                    com.nexus.backend.model.Batch b = allBatches.stream()
                        .filter(x -> x.getBatchName() != null && x.getBatchName().equalsIgnoreCase(finalName))
                        .findFirst().orElse(null);
                    enr.setBatchName(finalName);
                    enr.setBatchId(b != null ? b.getId() : null);
                }
            }

            if (body.containsKey("paymentStatus") && body.get("paymentStatus") != null)
                enr.setPaymentStatus(body.get("paymentStatus").toString());
            if (body.containsKey("enrollmentDate") && body.get("enrollmentDate") != null)
                enr.setEnrollmentDate(body.get("enrollmentDate").toString());

            enrollmentRepository.save(enr);
        }

        return ResponseEntity.ok(ApiResponse.ok("Student updated successfully", null));
    }

    @PutMapping("/students/{id}/change-batch")
    @Transactional
    public ResponseEntity<ApiResponse> changeStudentBatch(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        String courseTitle = body.get("courseTitle") != null ? body.get("courseTitle").toString().trim() : null;
        String batchName = body.get("batchName") != null ? body.get("batchName").toString().trim() : null;
        Long batchId = body.get("batchId") != null && !body.get("batchId").toString().isBlank()
            ? Long.valueOf(body.get("batchId").toString()) : null;
        Long enrollmentId = body.get("enrollmentId") != null && !body.get("enrollmentId").toString().isBlank()
            ? Long.valueOf(body.get("enrollmentId").toString()) : null;

        List<Enrollment> enrollments = enrollmentRepository.findByStudent(s);
        Enrollment target = null;
        if (enrollmentId != null) {
            target = enrollments.stream().filter(e -> e.getId().equals(enrollmentId)).findFirst().orElse(null);
        }
        if (target == null && courseTitle != null && !courseTitle.isBlank()) {
            target = enrollments.stream()
                .filter(e -> e.getCourseTitle() != null && e.getCourseTitle().equalsIgnoreCase(courseTitle))
                .findFirst().orElse(null);
        }
        if (target == null && !enrollments.isEmpty()) {
            target = enrollments.get(0);
        }

        if (target == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No enrollment found for student"));
        }

        // Resolve batch
        if (batchId != null) {
            com.nexus.backend.model.Batch b = batchRepository.findById(batchId).orElse(null);
            if (b != null) {
                target.setBatchId(b.getId());
                target.setBatchName(b.getBatchName());
            } else {
                target.setBatchId(null);
                target.setBatchName(batchName);
            }
        } else if (batchName != null && !batchName.isBlank() && !batchName.equalsIgnoreCase("No Batch") && !batchName.contains("No Batch")) {
            com.nexus.backend.model.Batch b = batchRepository.findAll().stream()
                .filter(x -> x.getBatchName() != null && x.getBatchName().equalsIgnoreCase(batchName.trim()))
                .findFirst().orElse(null);
            target.setBatchName(batchName.trim());
            target.setBatchId(b != null ? b.getId() : null);
        } else {
            // Cleared / No batch
            target.setBatchName(null);
            target.setBatchId(null);
        }

        enrollmentRepository.save(target);
        return ResponseEntity.ok(ApiResponse.ok("Batch changed successfully", target));
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
        String trimmedCourseTitle = courseTitle.trim();
        List<Enrollment> existingEnrollments = enrollmentRepository.findByStudent(s);
        Enrollment existingMatch = existingEnrollments.stream()
            .filter(en -> en.getCourseTitle() != null && en.getCourseTitle().trim().equalsIgnoreCase(trimmedCourseTitle))
            .findFirst().orElse(null);

        Enrollment e = existingMatch != null ? existingMatch : new Enrollment();
        if (existingMatch == null) {
            e.setStudent(s);
            e.setCourseTitle(trimmedCourseTitle);
            e.setEnrollmentDate(body.getOrDefault("enrollmentDate", java.time.LocalDate.now().toString()));
            e.setPaymentStatus(body.getOrDefault("paymentStatus", "Pending"));
        } else {
            if (body.containsKey("enrollmentDate") && body.get("enrollmentDate") != null && !body.get("enrollmentDate").isBlank()) {
                e.setEnrollmentDate(body.get("enrollmentDate"));
            }
            if (body.containsKey("paymentStatus") && body.get("paymentStatus") != null && !body.get("paymentStatus").isBlank()) {
                e.setPaymentStatus(body.get("paymentStatus"));
            }
        }

        String batchName = body.get("batchName");
        if (batchName != null && !batchName.isBlank() && !batchName.equalsIgnoreCase("No Batch") && !batchName.contains("No Batch")) {
            e.setBatchName(batchName.trim());
            com.nexus.backend.model.Batch b = batchRepository.findAll().stream()
                .filter(x -> x.getBatchName() != null && x.getBatchName().equalsIgnoreCase(batchName.trim()))
                .findFirst().orElse(null);
            e.setBatchId(b != null ? b.getId() : null);
        } else if (batchName != null && (batchName.isBlank() || batchName.equalsIgnoreCase("No Batch") || batchName.contains("No Batch"))) {
            e.setBatchName(null);
            e.setBatchId(null);
        }
        if (body.containsKey("batchId") && body.get("batchId") != null && !body.get("batchId").isBlank()) {
            e.setBatchId(Long.valueOf(body.get("batchId")));
        }

        enrollmentRepository.save(e);
        String studentName = s.getUser() != null ? s.getUser().getName() : "A student";
        // Find the teacher assigned to this course via batch instructor name
        String teacherEmail = batchRepository.findAll().stream()
            .filter(b -> b.getSelectCourse() != null && b.getSelectCourse().equalsIgnoreCase(courseTitle)
                && b.getInstructor() != null && !b.getInstructor().isBlank())
            .findFirst()
            .map(b -> userRepository.findAll().stream()
                .filter(u -> u.getName() != null && u.getName().equalsIgnoreCase(b.getInstructor()))
                .map(u -> u.getEmail()).findFirst().orElse(null))
            .orElse(null);
        if (teacherEmail != null) {
            appNotificationService.notifyEnrollment(studentName, courseTitle, teacherEmail);
            long total = enrollmentRepository.countByCourseTitleIgnoreCase(courseTitle);
            if (total % 100 == 0) appNotificationService.notifyMilestone(courseTitle, (int) total, teacherEmail);
        }
        return ResponseEntity.ok(ApiResponse.ok("Student enrolled in " + courseTitle, null));
    }

    @DeleteMapping("/students/{id}")
    @Transactional
    @SuppressWarnings("null")
    public ResponseEntity<ApiResponse> deleteStudent(@PathVariable Long id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        var user = s.getUser();
        enrollmentRepository.deleteLegacyEnrollmentsByStudentId(s.getId());
        enrollmentRepository.deleteByStudent(s);
        studentRepository.delete(s);
        // Remove any teacher record linked to this user before deleting user
        if (user != null) {
            teacherRepository.findByUser(user).ifPresent(t -> {
                assignmentRepository.deleteAll(assignmentRepository.findByTeacher(t));
                teacherRepository.delete(t);
            });
            userRepository.delete(user);
        }
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
        if (body.containsKey("phone")) {
            String phone = body.get("phone") != null ? body.get("phone").trim() : "";
            if (!phone.isBlank() && !phone.matches("^[6-9]\\d{9}$")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid mobile number. Must be a 10-digit number starting with 6, 7, 8, or 9."));
            }
            t.getUser().setPhone(phone);
            t.setPhone(phone);
        }
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

    // ── Password Reset Requests Endpoints for Admin ──
    @GetMapping("/password-resets")
    public ResponseEntity<ApiResponse> getPasswordResetRequests() {
        List<com.nexus.backend.model.PasswordResetRequest> list = passwordResetRequestRepository.findAllByOrderByCreatedAtDesc();
        long pendingCount = list.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        Map<String, Object> result = new HashMap<>();
        result.put("requests", list);
        result.put("pendingCount", pendingCount);
        result.put("totalCount", list.size());
        return ResponseEntity.ok(ApiResponse.ok("Password reset requests fetched", result));
    }

    @PutMapping("/password-resets/{id}/resolve")
    public ResponseEntity<ApiResponse> resolvePasswordReset(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            org.springframework.security.core.Authentication auth) {

        com.nexus.backend.model.PasswordResetRequest req = passwordResetRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Password reset request not found"));

        String adminName = auth != null && auth.getName() != null ? auth.getName() : "Admin";
        req.setStatus("RESOLVED");
        req.setResolvedAt(java.time.LocalDateTime.now());
        req.setResolvedBy(adminName);
        passwordResetRequestRepository.save(req);

        // If newPassword provided, update user's password directly
        if (body != null && body.containsKey("newPassword") && !body.get("newPassword").isBlank()) {
            String newPass = body.get("newPassword").trim();
            userRepository.findByEmailIgnoreCase(req.getEmail())
                    .or(() -> userRepository.findByEmail(req.getEmail()))
                    .ifPresent(u -> {
                        u.setPassword(passwordEncoder.encode(newPass));
                        userRepository.save(u);
                    });
        }

        return ResponseEntity.ok(ApiResponse.ok("Password reset request marked as resolved", req));
    }

    @DeleteMapping("/password-resets/{id}")
    public ResponseEntity<ApiResponse> deletePasswordResetRequest(@PathVariable Long id) {
        passwordResetRequestRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Password reset request deleted", null));
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
