package com.nexus.backend.model;

import com.nexus.backend.enums.BatchStatus;
import com.nexus.backend.enums.ClassDay;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a training batch created by an admin.
 * Stored in the "Batches" table in the database.
 * A batch groups students under a specific course, instructor, schedule, and date range.
 * Each batch has a status: UPCOMING, ACTIVE, or COMPLETED.
 */
@Entity
@Data
@Table(name = "Batches")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Batch {

    // Auto-incremented unique ID for each batch
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Name of the batch (e.g., "Batch A - Morning", "Java Batch 2026")
    private String batchName;

    // The course assigned to this batch (e.g., "Full Stack Web Development")
    private String selectCourse;

    // Name of the instructor assigned to teach this batch
    private String instructor;

    // Date when the batch classes start
    private LocalDate startDate;

    // Date when the batch classes end
    private LocalDate endDate;

    // List of days when classes are held (e.g., MON, WED, FRI)
    // Stored as a separate collection table (batch_class_days)
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<ClassDay> classDays;

    // Current status of the batch: UPCOMING, ACTIVE, or COMPLETED
    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    // Timestamp of when this batch record was created (auto-set to now)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Class timing string e.g. "2:00 PM - 3:30 PM"
    private String classTimings;

    // Batch duration e.g. "3 Months"
    private String duration;

    // Google Meet link for online classes
    private String googleMeetLink;
}
