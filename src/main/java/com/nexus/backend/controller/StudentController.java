package com.nexus.backend.controller;

import com.nexus.backend.model.*;
import com.nexus.backend.repository.*;
import com.nexus.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRecordingRepository recordingRepository;
    private final StudyMaterialRepository materialRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SecuritySettingsRepository securitySettingsRepository;

    private final EmailService emailService;

    @Value("${nexus.enquiry.admin-email:jadhavruchita27@gmail.com}")
    private String adminEmail;

    private Student getCurrentStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return null;
        return studentRepository.findByUser(userOpt.get()).orElse(null);
    }

    /** Called on login/logout to update online status */
    @PutMapping("/activity-status")
    @Transactional
    public ResponseEntity<Map<String, Object>> setActivityStatus(@RequestBody Map<String, Object> body) {
        Student student = getCurrentStudent();
        if (student == null) return ResponseEntity.status(401).build();
        boolean online = Boolean.TRUE.equals(body.get("online"));
        SecuritySettings settings = securitySettingsRepository.findByUserId(student.getUser().getId())
                .orElseGet(() -> { SecuritySettings s = new SecuritySettings(); s.setUserId(student.getUser().getId()); return s; });
        settings.setOnline(online);
        securitySettingsRepository.save(settings);
        return ResponseEntity.ok(java.util.Map.of("success", true));
    }

    /** Update activity status privacy setting */
    @PutMapping("/privacy-settings")
    @Transactional
    public ResponseEntity<Map<String, Object>> updatePrivacySettings(@RequestBody Map<String, Object> body) {
        Student student = getCurrentStudent();
        if (student == null) return ResponseEntity.status(401).build();
        SecuritySettings settings = securitySettingsRepository.findByUserId(student.getUser().getId())
                .orElseGet(() -> { SecuritySettings s = new SecuritySettings(); s.setUserId(student.getUser().getId()); return s; });
        if (body.containsKey("activityStatusEnabled"))
            settings.setActivityStatusEnabled(Boolean.TRUE.equals(body.get("activityStatusEnabled")));
        securitySettingsRepository.save(settings);
        return ResponseEntity.ok(java.util.Map.of("success", true));
    }

    /** Get student privacy settings */
    @GetMapping("/privacy-settings")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPrivacySettings() {
        Student student = getCurrentStudent();
        if (student == null) return ResponseEntity.status(401).build();
        SecuritySettings settings = securitySettingsRepository.findByUserId(student.getUser().getId())
                .orElseGet(() -> { SecuritySettings s = new SecuritySettings(); s.setUserId(student.getUser().getId()); return securitySettingsRepository.save(s); });
        return ResponseEntity.ok(java.util.Map.of("success", true, "data",
                java.util.Map.of("activityStatusEnabled", settings.isActivityStatusEnabled())));
    }

    /** Student sends a support message — auto-fills name/email/phone from JWT */
    @PostMapping("/support-message")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> sendSupportMessage(@RequestBody Map<String, String> body) {
        Student student = getCurrentStudent();
        if (student == null) return ResponseEntity.status(401).build();
        String subject = body.get("subject");
        String message = body.get("message");
        if (subject == null || subject.isBlank() || message == null || message.isBlank())
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Subject and message are required"));
        String name = student.getName() != null ? student.getName() : student.getUser().getName();
        String email = student.getEmail() != null ? student.getEmail() : student.getUser().getEmail();
        String phone = student.getPhone() != null ? student.getPhone() : (student.getUser().getPhone() != null ? student.getUser().getPhone() : "");
        emailService.sendSupportMessage(adminEmail, name, email, phone, subject, message);
        return ResponseEntity.ok(Map.of("success", true, "message", "Message sent successfully"));
    }

    /** Student self-update: name, phone, city, state only */
    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, String> body) {
        Student student = getCurrentStudent();
        if (student == null) return ResponseEntity.status(401).build();
        if (body.containsKey("firstName") && body.containsKey("lastName")) {
            String fullName = body.get("firstName") + " " + body.get("lastName");
            student.setName(fullName);
            student.getUser().setName(fullName);
        }
        if (body.containsKey("phone")) { student.setPhone(body.get("phone")); student.getUser().setPhone(body.get("phone")); }
        if (body.containsKey("city")) student.setCity(body.get("city"));
        if (body.containsKey("state")) student.setState(body.get("state"));
        userRepository.save(student.getUser());
        studentRepository.save(student);
        return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully"));
    }

    /** Returns the logged-in student's profile */
    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getProfile() {
        Student student = getCurrentStudent();
        if (student == null) return ResponseEntity.status(401).build();
        User user = student.getUser();
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", student.getId());
        dto.put("name", student.getName() != null && !student.getName().isBlank() ? student.getName() : user.getName());
        dto.put("email", student.getEmail() != null && !student.getEmail().isBlank() ? student.getEmail() : user.getEmail());
        dto.put("phone", student.getPhone() != null ? student.getPhone() : (user.getPhone() != null ? user.getPhone() : ""));
        dto.put("city", student.getCity() != null ? student.getCity() : "");
        dto.put("state", student.getState() != null ? student.getState() : "");
        dto.put("street", student.getStreet() != null ? student.getStreet() : "");
        dto.put("pinCode", student.getPinCode() != null ? student.getPinCode() : "");
        dto.put("joinedDate", user.getCreatedAt() != null
            ? user.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")) : "");
        return ResponseEntity.ok(Map.of("success", true, "data", dto));
    }

    /** Returns upcoming classes in the next 24 hours for the student's enrolled courses */
    @GetMapping("/upcoming-classes")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getUpcomingClasses() {
        Student student = getCurrentStudent();
        if (student == null) return ResponseEntity.status(401).build();
        List<String> enrolledCourses = enrollmentRepository.findByStudent(student)
                .stream().map(Enrollment::getCourseTitle).collect(Collectors.toList());
        java.time.DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();
        java.time.DayOfWeek tomorrow = today.plus(1);
        com.nexus.backend.enums.ClassDay todayDay = com.nexus.backend.enums.ClassDay
                .valueOf(today.name().substring(0, 3));
        com.nexus.backend.enums.ClassDay tomorrowDay = com.nexus.backend.enums.ClassDay
                .valueOf(tomorrow.name().substring(0, 3));
        List<Map<String, Object>> result = batchRepository.findAll().stream()
            .filter(b -> b.getSelectCourse() != null
                && enrolledCourses.stream().anyMatch(c -> c.equalsIgnoreCase(b.getSelectCourse()))
                && b.getClassDays() != null
                && (b.getClassDays().contains(todayDay) || b.getClassDays().contains(tomorrowDay)))
            .map(b -> {
                Map<String, Object> dto = new java.util.HashMap<>();
                dto.put("course", b.getSelectCourse());
                dto.put("batchName", b.getBatchName());
                dto.put("classTimings", b.getClassTimings() != null ? b.getClassTimings() : "");
                dto.put("isToday", b.getClassDays().contains(todayDay));
                dto.put("instructor", b.getInstructor() != null ? b.getInstructor() : "");
                return dto;
            }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** Returns enrolled courses with batch & status info */
    @GetMapping("/enrollments")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getEnrollments() {
        Student student = getCurrentStudent();
        if (student == null) return ResponseEntity.status(401).build();

        List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
        List<Map<String, Object>> result = enrollments.stream().map(e -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", e.getId());
            dto.put("courseTitle", e.getCourseTitle());
            dto.put("enrollmentDate", e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "");
            dto.put("paymentStatus", e.getPaymentStatus() != null ? e.getPaymentStatus() : "");

            // Find matching batch
            List<Batch> batches = batchRepository.findAll().stream()
                    .filter(b -> b.getSelectCourse() != null &&
                            b.getSelectCourse().equalsIgnoreCase(e.getCourseTitle()))
                    .collect(Collectors.toList());

            if (!batches.isEmpty()) {
                Batch batch = batches.get(0);
                dto.put("batchName", batch.getBatchName());
                dto.put("batchId", batch.getId());
                dto.put("instructor", batch.getInstructor());
                dto.put("startDate", batch.getStartDate() != null ? batch.getStartDate().toString() : "");
                dto.put("endDate", batch.getEndDate() != null ? batch.getEndDate().toString() : "");
                dto.put("status", batch.getStatus() != null ? batch.getStatus().name() : "ACTIVE");
                List<String> days = batch.getClassDays() != null
                        ? batch.getClassDays().stream().map(Enum::name).collect(Collectors.toList())
                        : Collections.emptyList();
                dto.put("classDays", days);
                // Get classTimings from Batch first, fallback to Course
                String timings = batch.getClassTimings() != null && !batch.getClassTimings().isBlank()
                        ? batch.getClassTimings()
                        : courseRepository.findAll().stream()
                            .filter(c -> c.getTitle() != null && c.getTitle().equalsIgnoreCase(e.getCourseTitle()))
                            .map(c -> c.getClassTimings() != null ? c.getClassTimings() : "")
                            .findFirst().orElse("");
                dto.put("classTimings", timings);
                // Also include googleMeetLink from course
                String meetLink = courseRepository.findAll().stream()
                        .filter(c -> c.getTitle() != null && c.getTitle().equalsIgnoreCase(e.getCourseTitle()))
                        .map(c -> c.getGoogleMeetLink() != null ? c.getGoogleMeetLink() : (c.getMeetLink() != null ? c.getMeetLink() : ""))
                        .findFirst().orElse("");
                dto.put("googleMeetLink", meetLink);
            } else {
                dto.put("batchName", "");
                dto.put("instructor", "");
                dto.put("status", "ACTIVE");
                dto.put("classDays", Collections.emptyList());
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /** Returns recordings for courses the student is enrolled in */
    @GetMapping("/recordings")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getRecordings() {
        Student student = getCurrentStudent();
        if (student == null) return ResponseEntity.status(401).build();

        List<String> enrolledCourses = enrollmentRepository.findByStudent(student)
                .stream().map(Enrollment::getCourseTitle).collect(Collectors.toList());

        if (enrolledCourses.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

        List<ClassRecording> recordings = enrolledCourses.stream()
                .flatMap(course -> recordingRepository.findByCourseIgnoreCase(course).stream())
                .sorted(Comparator.comparing(ClassRecording::getUploadedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // Build instructor name map: course -> instructor name from batch
        Map<String, String> courseInstructor = new HashMap<>();
        batchRepository.findAll().forEach(b -> {
            if (b.getSelectCourse() != null && b.getInstructor() != null) {
                courseInstructor.putIfAbsent(b.getSelectCourse().toLowerCase(), b.getInstructor());
            }
        });

        List<Map<String, Object>> result = recordings.stream().map(r -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", r.getId());
            dto.put("title", r.getTitle());
            dto.put("description", r.getDescription());
            dto.put("course", r.getCourse());
            dto.put("batch", r.getBatch());
            dto.put("classDate", r.getClassDate() != null ? r.getClassDate().toString() : null);
            dto.put("duration", r.getDuration());
            dto.put("fileName", r.getFileName());
            dto.put("fileSize", r.getFileSize());
            dto.put("uploadedAt", r.getUploadedAt() != null ? r.getUploadedAt().toString() : null);
            // Resolve instructor name from batch, fallback to email prefix
            String instructor = courseInstructor.get(r.getCourse() != null ? r.getCourse().toLowerCase() : "");
            if (instructor == null && r.getUploadedByEmail() != null) {
                instructor = r.getUploadedByEmail().split("@")[0].replace(".", " ");
            }
            dto.put("instructor", instructor != null ? instructor : "");
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /** Returns study materials for courses the student is enrolled in */
    @GetMapping("/materials")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StudyMaterial>> getMaterials() {
        Student student = getCurrentStudent();
        if (student == null) return ResponseEntity.status(401).build();

        List<String> enrolledCourses = enrollmentRepository.findByStudent(student)
                .stream().map(Enrollment::getCourseTitle).collect(Collectors.toList());

        if (enrolledCourses.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

        List<StudyMaterial> materials = materialRepository.findAll().stream()
                .filter(m -> enrolledCourses.stream()
                        .anyMatch(c -> c.equalsIgnoreCase(m.getCourse())))
                .sorted(Comparator.comparing(StudyMaterial::getUploadedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(materials);
    }
}
