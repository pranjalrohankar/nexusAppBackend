package com.nexus.backend.dto;

import com.nexus.backend.model.Course;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDto {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    private String category;

    private String description;

    private String duration; // e.g. "3 Months"

    private LocalDate startDate;

    private LocalDate endDate;

    private String classTimings; // e.g. "8:00 PM - 9:30 PM"

    private String classDays; // e.g. "Mon, Wed, Fri"

    private String instructor;

    private String syllabusTopics;

    private String whatYouWillLearn;

    private String googleMeetLink;

    private Integer totalSessions;

    @PositiveOrZero
    private Integer maxCapacity;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal price;

    private Course.Status status;
}
