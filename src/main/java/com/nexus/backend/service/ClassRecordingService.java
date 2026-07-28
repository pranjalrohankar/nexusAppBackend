package com.nexus.backend.service;

import com.nexus.backend.model.ClassRecording;
import com.nexus.backend.repository.ClassRecordingRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ClassRecordingService {

    private final ClassRecordingRepository repository;

    public ClassRecordingService(ClassRecordingRepository repository) {
        this.repository = repository;
    }

    public ClassRecording uploadRecording(
            MultipartFile file,
            String title,
            String description,
            LocalDate classDate,
            String duration,
            String course,
            String batch,
            String uploadedByEmail,
            String uploadedByRole
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a video file.");
        }

        String contentType = file.getContentType();
        if (!isAllowedVideo(contentType)) {
            throw new IllegalArgumentException("Only MP4, MKV, MOV and AVI videos are allowed.");
        }

        String uploadDir = "uploads/recordings";
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        String safeFileName = Objects.requireNonNullElse(file.getOriginalFilename(), "recording").replaceAll("\\s+", "_");
        String fileName = System.currentTimeMillis() + "_" + safeFileName;
        Path filePath = uploadPath.resolve(fileName);

        // Stream directly to disk — no RAM load, works for 1-2 hour videos
        file.transferTo(filePath.toAbsolutePath());

        ClassRecording recording = new ClassRecording();
        recording.setTitle(title);
        recording.setDescription(description);
        recording.setClassDate(classDate);
        recording.setDuration(duration);
        recording.setCourse(course);
        recording.setBatch(batch);
        recording.setFileName(fileName);
        recording.setFilePath("uploads/recordings/" + fileName);
        recording.setFileSize(file.getSize());
        recording.setFileType(contentType);
        recording.setUploadedByEmail(uploadedByEmail);
        recording.setUploadedByRole(uploadedByRole);
        recording.setUploadedAt(LocalDateTime.now());

        ClassRecording saved = repository.save(recording);
        saved.setFileUrl("/api/recordings/stream/" + saved.getId());
        return repository.save(saved);
    }

    public static Path resolveFilePath(String storedFilePath, String fileName) {
        if (fileName != null && !fileName.isBlank()) {
            Path p1 = Paths.get("uploads", "recordings", fileName);
            if (Files.exists(p1)) return p1;
        }
        if (storedFilePath != null && !storedFilePath.isBlank()) {
            Path p2 = Paths.get(storedFilePath);
            if (Files.exists(p2)) return p2;

            Path nameOnly = p2.getFileName();
            if (nameOnly != null) {
                Path p3 = Paths.get("uploads", "recordings", nameOnly.toString());
                if (Files.exists(p3)) return p3;
            }
        }
        return Paths.get("uploads", "recordings", fileName != null ? fileName : "");
    }

    public List<ClassRecording> getAllRecordings(String currentEmail, String currentRole) {
        List<ClassRecording> recordings = repository.findAll();
        if (currentEmail == null || currentEmail.isBlank()) return recordings;
        if ("ADMIN".equalsIgnoreCase(currentRole)) return recordings;
        return recordings.stream()
                .filter(r -> currentEmail.equalsIgnoreCase(r.getUploadedByEmail()))
                .toList();
    }

    public ClassRecording getRecordingById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recording not found."));
    }

    public void deleteRecording(Long id) throws IOException {
        ClassRecording recording = getRecordingById(id);

        Path p = resolveFilePath(recording.getFilePath(), recording.getFileName());
        Files.deleteIfExists(p);

        repository.deleteById(id);
    }

    public void deleteAllRecordings() {
        repository.deleteAll();
    }

    private boolean isAllowedVideo(String contentType) {
        if (contentType == null) return false;
        return contentType.equals("video/mp4")
                || contentType.equals("video/x-matroska")
                || contentType.equals("video/quicktime")
                || contentType.equals("video/x-msvideo");
    }
}
