package com.nexus.backend.controller;

import com.nexus.backend.dto.TeacherStatsDTO;
import com.nexus.backend.model.*;
import com.nexus.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TeacherController {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TeacherCourseAssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getMyProfile() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = (String) auth.getPrincipal();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.status(404).body(error);
            }
            Optional<Teacher> teacherOpt = teacherRepository.findByUser(userOpt.get());
            if (teacherOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Teacher profile not found");
                return ResponseEntity.status(404).body(error);
            }
            TeacherStatsDTO stats = buildTeacherStats(teacherOpt.get());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to fetch profile: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllTeachers() {
        try {
            List<Teacher> teachers = teacherRepository.findAll();
            List<TeacherStatsDTO> teacherStats = teachers.stream()
                .map(this::buildTeacherStats)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", teacherStats);
            response.put("totalCount", teachers.size());
            response.put("activeCount", teacherStats.stream()
                .filter(t -> "Active".equals(t.getStatus()))
                .count());
            response.put("totalStudents", (int) enrollmentRepository.count());
            
            return ResponseEntity.ok(response);
        // amazonq-ignore-next-line
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to fetch teachers: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTeacherById(@PathVariable Long id) {
        try {
            Optional<Teacher> teacher = teacherRepository.findById(id);
            if (teacher.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Teacher not found");
                return ResponseEntity.status(404).body(error);
            }

            // amazonq-ignore-next-line
            // amazonq-ignore-next-line
            TeacherStatsDTO stats = buildTeacherStats(teacher.get());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
        // amazonq-ignore-next-line
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to fetch teacher: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PutMapping("/{id}")
    // amazonq-ignore-next-line
    public ResponseEntity<Map<String, Object>> updateTeacher(
        @PathVariable Long id,
        @RequestBody Map<String, Object> updates
    ) {
        try {
            Optional<Teacher> teacherOpt = teacherRepository.findById(id);
            if (teacherOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Teacher not found");
                return ResponseEntity.status(404).body(error);
            }

            // amazonq-ignore-next-line
            Teacher teacher = teacherOpt.get();
            
            // Update basic fields
            if (updates.containsKey("firstName") && updates.containsKey("lastName")) {
                String fullName = updates.get("firstName") + " " + updates.get("lastName");
                teacher.setName(fullName);
                if (teacher.getUser() != null) {
                    teacher.getUser().setName(fullName);
                }
            }
            if (updates.containsKey("email")) {
                teacher.setEmail((String) updates.get("email"));
                if (teacher.getUser() != null) {
                    teacher.getUser().setEmail((String) updates.get("email"));
                }
            }
            if (updates.containsKey("phone")) {
                teacher.setPhone((String) updates.get("phone"));
                if (teacher.getUser() != null) {
                    teacher.getUser().setPhone((String) updates.get("phone"));
                }
            }
            if (updates.containsKey("dob")) teacher.setDob((String) updates.get("dob"));
            if (updates.containsKey("street")) teacher.setStreet((String) updates.get("street"));
            if (updates.containsKey("city")) teacher.setCity((String) updates.get("city"));
            if (updates.containsKey("state")) teacher.setState((String) updates.get("state"));
            if (updates.containsKey("pinCode")) teacher.setPinCode((String) updates.get("pinCode"));
            if (updates.containsKey("qualification")) teacher.setQualification((String) updates.get("qualification"));
            if (updates.containsKey("experience")) teacher.setExperience((String) updates.get("experience"));
            if (updates.containsKey("specialization")) teacher.setSpecialization((String) updates.get("specialization"));
            if (updates.containsKey("employmentType")) teacher.setEmploymentType((String) updates.get("employmentType"));

            teacherRepository.save(teacher);
            if (teacher.getUser() != null) {
                userRepository.save(teacher.getUser());
            }

            // Handle course assignments
            if (updates.containsKey("courseIds")) {
                @SuppressWarnings("unchecked")
                List<Integer> courseIds = (List<Integer>) updates.get("courseIds");
                updateTeacherCourses(teacher, courseIds);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Teacher updated successfully");
            response.put("data", buildTeacherStats(teacher));
            
            return ResponseEntity.ok(response);
        // amazonq-ignore-next-line
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to update teacher: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTeacher(@PathVariable Long id) {
        try {
            Optional<Teacher> teacherOpt = teacherRepository.findById(id);
            if (teacherOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Teacher not found");
                return ResponseEntity.status(404).body(error);
            }

            // amazonq-ignore-next-line
            Teacher teacher = teacherOpt.get();
            
            // Delete course assignments first
            List<TeacherCourseAssignment> assignments = assignmentRepository.findByTeacher(teacher);
            assignmentRepository.deleteAll(assignments);
            
            // Delete teacher
            teacherRepository.delete(teacher);
            
            // Optionally delete user account
            if (teacher.getUser() != null) {
                userRepository.delete(teacher.getUser());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Teacher deleted successfully");
            
            return ResponseEntity.ok(response);
        // amazonq-ignore-next-line
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to delete teacher: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{teacherId}/courses/{courseId}")
    public ResponseEntity<Map<String, Object>> assignCourse(
        @PathVariable Long teacherId,
        @PathVariable Long courseId
    ) {
        try {
            Optional<Teacher> teacherOpt = teacherRepository.findById(teacherId);
            Optional<Course> courseOpt = courseRepository.findById(courseId);

            if (teacherOpt.isEmpty() || courseOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Teacher or Course not found");
                return ResponseEntity.status(404).body(error);
            }

            // amazonq-ignore-next-line
            Teacher teacher = teacherOpt.get();
            // amazonq-ignore-next-line
            Course course = courseOpt.get();

            // Check if already assigned
            if (assignmentRepository.existsByTeacherAndCourse(teacher, course)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Course already assigned to this teacher");
                return ResponseEntity.status(400).body(error);
            }

            TeacherCourseAssignment assignment = TeacherCourseAssignment.builder()
                .teacher(teacher)
                .course(course)
                .assignedDate(LocalDate.now().toString())
                .build();

            assignmentRepository.save(assignment);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Course assigned successfully");
            
            return ResponseEntity.ok(response);
        // amazonq-ignore-next-line
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to assign course: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @DeleteMapping("/{teacherId}/courses/{courseId}")
    public ResponseEntity<Map<String, Object>> unassignCourse(
        @PathVariable Long teacherId,
        @PathVariable Long courseId
    ) {
        try {
            Optional<Teacher> teacherOpt = teacherRepository.findById(teacherId);
            Optional<Course> courseOpt = courseRepository.findById(courseId);

            if (teacherOpt.isEmpty() || courseOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Teacher or Course not found");
                return ResponseEntity.status(404).body(error);
            }

            // amazonq-ignore-next-line
            assignmentRepository.deleteByTeacherAndCourse(teacherOpt.get(), courseOpt.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Course unassigned successfully");
            
            return ResponseEntity.ok(response);
        // amazonq-ignore-next-line
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to unassign course: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private TeacherStatsDTO buildTeacherStats(Teacher teacher) {
        List<TeacherCourseAssignment> assignments = assignmentRepository.findByTeacher(teacher);
        List<TeacherStatsDTO.CourseDTO> courseDTOs = assignments.stream()
            .map(a -> TeacherStatsDTO.CourseDTO.builder()
                .courseId(a.getCourse().getId())
                .title(a.getCourse().getTitle())
                .category(a.getCourse().getCategory())
                .build())
            .collect(Collectors.toList());

        int studentsCount = 0;
        if (!assignments.isEmpty()) {
            List<String> courseTitles = assignments.stream()
                .map(a -> a.getCourse().getTitle())
                .collect(Collectors.toList());
            studentsCount = enrollmentRepository.countByCourseTitleIn(courseTitles);
        }

        String status = "Active";

        return TeacherStatsDTO.builder()
            .teacherId(teacher.getId())
            .name(teacher.getName())
            .email(teacher.getEmail())
            .phone(teacher.getPhone())
            .joinDate(teacher.getJoinDate())
            .status(status)
            .qualification(teacher.getQualification())
            .experience(teacher.getExperience())
            .specialization(teacher.getSpecialization())
            .employmentType(teacher.getEmploymentType())
            .coursesCount(assignments.size())
            .studentsCount(studentsCount)
            .assignedCourses(courseDTOs)
            .street(teacher.getStreet())
            .city(teacher.getCity())
            .state(teacher.getState())
            .pinCode(teacher.getPinCode())
            .build();
    }

    private void updateTeacherCourses(Teacher teacher, List<Integer> courseIds) {
        // Remove existing assignments
        List<TeacherCourseAssignment> existing = assignmentRepository.findByTeacher(teacher);
        assignmentRepository.deleteAll(existing);

        // Add new assignments
        for (Integer courseId : courseIds) {
            Optional<Course> courseOpt = courseRepository.findById(courseId.longValue());
            if (courseOpt.isPresent()) {
                TeacherCourseAssignment assignment = TeacherCourseAssignment.builder()
                    .teacher(teacher)
                    .course(courseOpt.get())
                    .assignedDate(LocalDate.now().toString())
                    .build();
                assignmentRepository.save(assignment);
            }
        }
    }
}
