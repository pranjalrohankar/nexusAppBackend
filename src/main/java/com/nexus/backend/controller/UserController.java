package com.nexus.backend.controller;

import com.nexus.backend.dto.ApiResponse;
import com.nexus.backend.dto.UserProfileDto;
import com.nexus.backend.model.Student;
import com.nexus.backend.model.Teacher;
import com.nexus.backend.model.User;
import com.nexus.backend.repository.StudentRepository;
import com.nexus.backend.repository.TeacherCourseAssignmentRepository;
import com.nexus.backend.repository.TeacherRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        String email = auth.getName();
        if (email == null) return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * GET /api/users/me - Returns complete user profile of the authenticated user
     */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getMe() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        UserProfileDto dto;
        if (user.getRole() == User.Role.STUDENT) {
            Optional<Student> studentOpt = studentRepository.findByUser(user);
            dto = studentOpt.map(UserProfileDto::fromStudent).orElseGet(() -> UserProfileDto.fromUser(user));
        } else if (user.getRole() == User.Role.TEACHER) {
            Optional<Teacher> teacherOpt = teacherRepository.findByUser(user);
            if (teacherOpt.isPresent()) {
                List<String> assigned = teacherCourseAssignmentRepository.findByTeacher(teacherOpt.get()).stream()
                        .map(a -> a.getCourse().getTitle())
                        .collect(Collectors.toList());
                dto = UserProfileDto.fromTeacher(teacherOpt.get(), assigned);
            } else {
                dto = UserProfileDto.fromUser(user);
            }
        } else {
            dto = UserProfileDto.fromUser(user);
        }

        return ResponseEntity.ok(ApiResponse.ok("User profile fetched", dto));
    }

    /**
     * PUT /api/users/profile - Update own profile info
     */
    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<ApiResponse<?>> updateProfile(@RequestBody UserProfileDto updateDto) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        if (updateDto.getName() != null && !updateDto.getName().isBlank()) {
            user.setName(updateDto.getName().trim());
        }
        if (updateDto.getPhone() != null) {
            user.setPhone(updateDto.getPhone().trim());
        }
        userRepository.save(user);

        if (user.getRole() == User.Role.STUDENT) {
            studentRepository.findByUser(user).ifPresent(s -> {
                if (updateDto.getName() != null) s.setName(updateDto.getName());
                if (updateDto.getPhone() != null) s.setPhone(updateDto.getPhone());
                if (updateDto.getDob() != null) s.setDob(updateDto.getDob());
                if (updateDto.getStreet() != null) s.setStreet(updateDto.getStreet());
                if (updateDto.getCity() != null) s.setCity(updateDto.getCity());
                if (updateDto.getState() != null) s.setState(updateDto.getState());
                if (updateDto.getPinCode() != null) s.setPinCode(updateDto.getPinCode());
                if (updateDto.getGuardianName() != null) s.setGuardianName(updateDto.getGuardianName());
                if (updateDto.getGuardianPhone() != null) s.setGuardianPhone(updateDto.getGuardianPhone());
                studentRepository.save(s);
            });
        } else if (user.getRole() == User.Role.TEACHER) {
            teacherRepository.findByUser(user).ifPresent(t -> {
                if (updateDto.getName() != null) t.setName(updateDto.getName());
                if (updateDto.getPhone() != null) t.setPhone(updateDto.getPhone());
                if (updateDto.getDob() != null) t.setDob(updateDto.getDob());
                if (updateDto.getStreet() != null) t.setStreet(updateDto.getStreet());
                if (updateDto.getCity() != null) t.setCity(updateDto.getCity());
                if (updateDto.getState() != null) t.setState(updateDto.getState());
                if (updateDto.getPinCode() != null) t.setPinCode(updateDto.getPinCode());
                if (updateDto.getQualification() != null) t.setQualification(updateDto.getQualification());
                if (updateDto.getExperience() != null) t.setExperience(updateDto.getExperience());
                if (updateDto.getSpecialization() != null) t.setSpecialization(updateDto.getSpecialization());
                if (updateDto.getProfileImage() != null) t.setProfileImage(updateDto.getProfileImage());
                teacherRepository.save(t);
            });
        }

        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", null));
    }

    /**
     * PUT /api/users/change-password - Change current user password
     */
    @PutMapping("/change-password")
    @Transactional
    public ResponseEntity<ApiResponse<?>> changePassword(@RequestBody Map<String, String> body) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("New password must be at least 6 characters"));
        }

        if (currentPassword != null && !passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Current password does not match"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }

    /**
     * GET /api/users/all - List all users (Admin view)
     */
    @GetMapping("/all")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAllUsers() {
        List<UserProfileDto> list = userRepository.findAll().stream()
                .map(UserProfileDto::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("All users fetched", list));
    }

    /**
     * PUT /api/users/{id}/status - Toggle user active/inactive status
     */
    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<?>> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var opt = userRepository.findById(id);
        if (opt.isPresent()) {
            User user = opt.get();
            boolean active = body.get("active") != null ? Boolean.parseBoolean(body.get("active").toString()) : !user.isActive();
            user.setActive(active);
            userRepository.save(user);
            return ResponseEntity.ok(ApiResponse.ok("User status updated", Map.of("id", user.getId(), "active", user.isActive())));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("User not found"));
    }
}
