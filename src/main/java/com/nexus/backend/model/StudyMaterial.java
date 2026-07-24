package com.nexus.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String description;

    private String course;

    private String batch;

    private String topic;

    private String fileType;

    private String fileName;

    private String filePath;

    @Column(length = 1000)
    private String fileUrl;

    private String uploadedByEmail;

    private String uploadedByRole;

    private LocalDateTime uploadedAt;
}