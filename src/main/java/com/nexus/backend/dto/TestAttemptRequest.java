package com.nexus.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAttemptRequest {

    private Long testId;
    private String testTitle;

    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Integer marksObtained;
    private Integer totalMarks;

    @JsonProperty("answersJson")
    private String answersJson;

    @JsonProperty("answersText")
    private String answersText;

    @JsonProperty("solutionFileName")
    private String solutionFileName;

    @JsonProperty("solutionFileUri")
    private String solutionFileUri;

    private String status; // "PENDING", "GRADED"
    private String feedback;

    private String attemptDate;
    private String attemptTime;
}
