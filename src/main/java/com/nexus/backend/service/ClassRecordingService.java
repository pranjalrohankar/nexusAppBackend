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
            String details = file == null ? "null" : "size=" + file.getSize() + "; name=" + file.getOriginalFilename();
            throw new IllegalArgumentException("Please select video file. File details: " + details);
        }

        String contentType = file.getContentType();

        if (!isAllowedVideo(contentType)) {
            throw new IllegalArgumentException("Only MP4, MKV, MOV and AVI videos are allowed.");
        }

        String uploadDir = "uploads/recordings/";

        Files.createDirectories(Paths.get(uploadDir));

        String originalFileName = file.getOriginalFilename();
        String safeFileName = originalFileName.replaceAll("\\s+", "_");
        String fileName = System.currentTimeMillis() + "_" + safeFileName;

        Path filePath = Paths.get(uploadDir + fileName);

        Files.write(filePath, file.getBytes());

        ClassRecording recording = new ClassRecording();
        recording.setTitle(title);
        recording.setDescription(description);
        recording.setClassDate(classDate);
        recording.setDuration(duration);
        recording.setCourse(course);
        recording.setBatch(batch);
        recording.setFileName(fileName);
        recording.setFilePath(filePath.toString());
        recording.setFileUrl("/uploads/recordings/" + fileName);
        recording.setFileSize(file.getSize());
        recording.setFileType(contentType);
        recording.setUploadedByEmail(uploadedByEmail);
        recording.setUploadedByRole(uploadedByRole);
        recording.setUploadedAt(LocalDateTime.now());

        return repository.save(recording);
    }

    public List<ClassRecording> getAllRecordings(String currentEmail, String currentRole) {
        List<ClassRecording> recordings = repository.findAll();

        if (currentEmail == null || currentEmail.isBlank()) {
            return recordings;
        }

        if ("ADMIN".equalsIgnoreCase(currentRole)) {
            return recordings;
        }

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

        if (recording.getFilePath() != null) {
            Files.deleteIfExists(Paths.get(recording.getFilePath()));
        }

        repository.deleteById(id);
    }

    private boolean isAllowedVideo(String contentType) {
        if (contentType == null) {
            return false;
        }

        return contentType.equals("video/mp4")
                || contentType.equals("video/x-matroska")
                || contentType.equals("video/quicktime")
                || contentType.equals("video/x-msvideo");
    }
}