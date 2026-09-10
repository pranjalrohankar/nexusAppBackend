package com.nexus.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_recording")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassRecording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private LocalDate classDate;

    private String duration; // HH:MM:SS

    @Column(nullable = false)
    private String course;

    @Column(nullable = false)
    private String batch;

    private String fileName;

    private String filePath;

    @Column(length = 1000)
    private String fileUrl;

    private Long fileSize;

    private String fileType;

    private String uploadedByEmail;

    private String uploadedByRole;

    private LocalDateTime uploadedAt;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data", columnDefinition = "bytea")
    private byte[] fileData;
}