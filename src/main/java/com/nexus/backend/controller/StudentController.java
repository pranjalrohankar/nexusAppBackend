package com.nexus.backend.controller;

import com.nexus.backend.model.*;
import com.nexus.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

    private Student getCurrentStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return null;
        return studentRepository.findByUser(userOpt.get()).orElse(null);
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
                // Get classTimings from Course
                String timings = courseRepository.findAll().stream()
                        .filter(c -> c.getTitle() != null && c.getTitle().equalsIgnoreCase(e.getCourseTitle()))
                        .map(c -> c.getClassTimings() != null ? c.getClassTimings() : "")
                        .findFirst().orElse("");
                dto.put("classTimings", timings);
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
