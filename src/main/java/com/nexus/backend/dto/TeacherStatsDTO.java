package com.nexus.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherStatsDTO {
    private Long teacherId;
    private String name;
    private String email;
    private String phone;
    private String joinDate;
    private String status;
    private String qualification;
    private String experience;
    private String specialization;
    private String employmentType;
    private Integer coursesCount;
    private Integer studentsCount;
    private List<CourseDTO> assignedCourses;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseDTO {
        private Long courseId;
        private String title;
        private String category;
    }
}
