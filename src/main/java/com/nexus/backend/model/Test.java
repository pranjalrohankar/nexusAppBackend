package com.nexus.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "course_title")
    private String courseTitle;

    @Column(name = "category")
    private String category;

    @Column(name = "duration")
    private String duration;

    @Column(name = "pass_score")
    private String passScore;

    @Column(name = "total_marks")
    private Integer totalMarks;

    @Column(name = "test_type")
    private String testType;

    @Column(name = "questions_count")
    private Integer questionsCount;

    @Column(name = "pdf_file_name")
    private String pdfFileName;

    @Column(name = "pdf_file_uri", columnDefinition = "TEXT")
    private String pdfFileUri;

    @Column(name = "pdf_instructions", columnDefinition = "TEXT")
    private String pdfInstructions;

    @Column(name = "questions_json", columnDefinition = "TEXT")
    private String questionsJson;

    @Column(name = "created_by_teacher_email")
    private String createdByTeacherEmail;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "test_date")
    private String testDate;

    @Column(name = "test_time")
    private String testTime;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
