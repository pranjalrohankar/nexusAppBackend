package com.nexus.backend.model;

import com.nexus.backend.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a study material uploaded by a teacher.
 * Stored in the "StudentMaterial" table in the database.
 * Teachers upload files (PDF, PPT, VIDEO, etc.) linked to a specific course and batch.
 * Students can then view and download these materials from their dashboard.
 */
@Entity
@Table(name = "StudentMaterial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class StudyMaterial {

    // Auto-incremented unique ID for each study material record
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Title of the material (e.g., "React Hooks Guide")
    private String Title;

    // Short description of what the material covers
    private String Description;

    // Course this material belongs to (e.g., "Full Stack Web Development")
    private String course;

    // Batch this material is assigned to (e.g., "Batch A")
    private String batch;

    // Type of file uploaded (e.g., PDF, PPT, VIDEO, ZIP)
    private String fileType;

    // Unique file name stored on the server (timestamp + original name)
    private String fileName;

    // Full path on the server where the file is stored
    private String filePath;

    // Timestamp of when the material was uploaded
    private LocalDateTime uploadedAt;
}
