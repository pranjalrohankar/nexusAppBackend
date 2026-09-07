package com.nexus.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nexus.backend.model.Test;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestDto {

    private Long id;
    private String title;
    private String testName;
    private String courseTitle;
    private String category;
    private String duration;
    private String passScore;
    private Integer totalMarks;
    private String testType; // "MCQ" or "PDF"
    private Integer questionsCount;

    private String pdfFileName;
    private String pdfFileUri;
    private String pdfInstructions;

    @JsonProperty("questionsJson")
    private String questionsJson;

    private String createdByTeacherEmail;
    private String createdByName;
    private String testDate;
    private String testTime;
    private LocalDateTime createdAt;

    public static TestDto fromEntity(Test test) {
        if (test == null) return null;
        return TestDto.builder()
                .id(test.getId())
                .title(test.getTestName())
                .testName(test.getTestName())
                .courseTitle(test.getCourseTitle())
                .category(test.getCategory())
                .duration(test.getDuration())
                .passScore(test.getPassScore())
                .totalMarks(test.getTotalMarks())
                .testType(test.getTestType() != null ? test.getTestType() : "MCQ")
                .questionsCount(test.getQuestionsCount())
                .pdfFileName(test.getPdfFileName())
                .pdfFileUri(test.getPdfFileUri())
                .pdfInstructions(test.getPdfInstructions())
                .questionsJson(test.getQuestionsJson())
                .createdByTeacherEmail(test.getCreatedByTeacherEmail())
                .createdByName(test.getCreatedByName())
                .testDate(test.getTestDate())
                .testTime(test.getTestTime())
                .createdAt(test.getCreatedAt())
                .build();
    }
}
