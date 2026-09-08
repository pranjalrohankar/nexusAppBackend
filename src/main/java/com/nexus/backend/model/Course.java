package com.nexus.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    @NotBlank
    private String title;

    @Column(name = "category")
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration")
    private String duration; // e.g. "3 Months"

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "class_timings")
    private String classTimings; // e.g. "8:00 PM - 9:30 PM"

    @Column(name = "class_days")
    private String classDays; // e.g. "Mon, Wed, Fri"

    @Column(name = "instructor")
    private String instructor;

    @Column(name = "syllabus_topics", columnDefinition = "TEXT")
    private String syllabusTopics;

    @Column(name = "what_you_will_learn", columnDefinition = "TEXT")
    private String whatYouWillLearn;

    @Column(name = "google_meet_link")
    private String googleMeetLink;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "max_capacity")
    @PositiveOrZero
    private Integer maxCapacity;

    @Column(name = "price", precision = 12, scale = 2)
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal price;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.DRAFT;

    @Builder.Default
    @Column(name = "auto_generate_meet_link")
    private Boolean autoGenerateMeetLink = Boolean.FALSE;

    @Column(name = "meet_link")
    private String meetLink;

    @Column(name = "covered_topics", columnDefinition = "TEXT")
    private String coveredTopics;

    public enum Status {
        ACTIVE,
        INACTIVE,
        DRAFT
    }

    @PrePersist
    @PreUpdate
    private void ensureMeetLinkIfRequired() {
        if (Boolean.TRUE.equals(autoGenerateMeetLink) && (meetLink == null || meetLink.isBlank())) {
            // simple deterministic-ish meet link token (not an official Google Meet generator)
            String token = UUID.randomUUID().toString().replace("-", "").substring(0, 11);
            this.meetLink = "https://meet.google.com/" + token;
        }
    }
}
