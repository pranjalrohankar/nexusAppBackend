package com.nexus.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexus.backend.model.Student;
import com.nexus.backend.model.Teacher;
import com.nexus.backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileDto {

    // User Base Information
    private Long id;
    private Long userId;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    // Profile & Address Details
    private String dob;
    private String street;
    private String city;
    private String state;
    private String pinCode;
    private String profileImage;

    // Student-Specific Details
    private Long studentId;
    private String guardianName;
    private String guardianPhone;
    private String course;
    private String enrollmentDate;
    private String paymentStatus;

    // Teacher-Specific Details
    private Long teacherId;
    private String qualification;
    private String experience;
    private String specialization;
    private String joinDate;
    private String employmentType;
    private List<String> assignedCourses;

    public static UserProfileDto fromUser(User user) {
        if (user == null) return null;
        return UserProfileDto.builder()
                .id(user.getId())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    public static UserProfileDto fromStudent(Student student) {
        if (student == null) return null;
        User user = student.getUser();
        UserProfileDtoBuilder builder = UserProfileDto.builder()
                .id(student.getId())
                .studentId(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .dob(student.getDob())
                .street(student.getStreet())
                .city(student.getCity())
                .state(student.getState())
                .pinCode(student.getPinCode())
                .guardianName(student.getGuardianName())
                .guardianPhone(student.getGuardianPhone())
                .course(student.getCourse())
                .enrollmentDate(student.getEnrollmentDate())
                .paymentStatus(student.getPaymentStatus());

        if (user != null) {
            builder.userId(user.getId())
                    .role(user.getRole() != null ? user.getRole().name() : "STUDENT")
                    .active(user.isActive())
                    .createdAt(user.getCreatedAt())
                    .lastLogin(user.getLastLogin());
            if (student.getName() == null) builder.name(user.getName());
            if (student.getEmail() == null) builder.email(user.getEmail());
            if (student.getPhone() == null) builder.phone(user.getPhone());
        } else {
            builder.role("STUDENT");
            builder.active(true);
        }

        return builder.build();
    }

    public static UserProfileDto fromTeacher(Teacher teacher, List<String> assignedCourses) {
        if (teacher == null) return null;
        User user = teacher.getUser();
        UserProfileDtoBuilder builder = UserProfileDto.builder()
                .id(teacher.getId())
                .teacherId(teacher.getId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .phone(teacher.getPhone())
                .dob(teacher.getDob())
                .street(teacher.getStreet())
                .city(teacher.getCity())
                .state(teacher.getState())
                .pinCode(teacher.getPinCode())
                .qualification(teacher.getQualification())
                .experience(teacher.getExperience())
                .specialization(teacher.getSpecialization())
                .joinDate(teacher.getJoinDate())
                .employmentType(teacher.getEmploymentType())
                .profileImage(teacher.getProfileImage())
                .assignedCourses(assignedCourses);

        if (user != null) {
            builder.userId(user.getId())
                    .role(user.getRole() != null ? user.getRole().name() : "TEACHER")
                    .active(user.isActive())
                    .createdAt(user.getCreatedAt())
                    .lastLogin(user.getLastLogin());
            if (teacher.getName() == null) builder.name(user.getName());
            if (teacher.getEmail() == null) builder.email(user.getEmail());
            if (teacher.getPhone() == null) builder.phone(user.getPhone());
        } else {
            builder.role("TEACHER");
            builder.active(true);
        }

        return builder.build();
    }
}
