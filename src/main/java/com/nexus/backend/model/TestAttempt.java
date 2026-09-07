package com.nexus.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    private Test test;

    @Column(name = "test_title")
    private String testTitle;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "student_email")
    private String studentEmail;

    @Column(name = "marks_obtained")
    private Integer marksObtained;

    @Column(name = "total_marks")
    private Integer totalMarks;

    @Column(name = "status")
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "answers_json", columnDefinition = "TEXT")
    private String answersJson;

    @Column(name = "answers_text", columnDefinition = "TEXT")
    private String answersText;

    @Column(name = "solution_file_name")
    private String solutionFileName;

    @Column(name = "solution_file_uri", columnDefinition = "TEXT")
    private String solutionFileUri;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "attempt_date")
    private String attemptDate;

    @Column(name = "attempt_time")
    private String attemptTime;

    @Column(name = "submitted_at")
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();
}
