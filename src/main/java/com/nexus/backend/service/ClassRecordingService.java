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
            String videoUrl,
            String title,
            String description,
            LocalDate classDate,
            String duration,
            String course,
            String batch,
            String uploadedByEmail,
            String uploadedByRole
    ) throws IOException {

        ClassRecording recording = new ClassRecording();
        recording.setTitle(title);
        recording.setDescription(description);
        recording.setClassDate(classDate);
        recording.setDuration(duration != null && !duration.isBlank() ? duration : "01:00:00");
        recording.setCourse(course);
        recording.setBatch(batch);
        recording.setUploadedByEmail(uploadedByEmail);
        recording.setUploadedByRole(uploadedByRole);
        recording.setUploadedAt(LocalDateTime.now());

        if (videoUrl != null && !videoUrl.isBlank()) {
            recording.setFileUrl(videoUrl.trim());
            recording.setFileName("Online Video Lecture");
            recording.setFileType("video/mp4");
            recording.setFileSize(0L);
            return repository.save(recording);
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a video file or provide a video link.");
        }

        String safeFileName = Objects.requireNonNullElse(file.getOriginalFilename(), "recording.mp4").replaceAll("\\s+", "_");
        String contentType = file.getContentType();
        if (!isAllowedVideo(contentType, safeFileName)) {
            throw new IllegalArgumentException("Uploaded file must be a valid video format (MP4, MOV, MKV, AVI, WebM).");
        }
        if (contentType == null || contentType.isBlank() || contentType.contains("octet-stream")) {
            contentType = safeFileName.toLowerCase().endsWith(".mov") ? "video/quicktime" : "video/mp4";
        }

        String uploadDir = "uploads/recordings";
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        String fileName = System.currentTimeMillis() + "_" + safeFileName;
        Path filePath = uploadPath.resolve(fileName);

        // Stream directly to disk — no RAM load, works for 1-2 hour videos
        file.transferTo(filePath.toAbsolutePath());

        recording.setFileName(fileName);
        recording.setFilePath("uploads/recordings/" + fileName);
        recording.setFileSize(file.getSize());
        recording.setFileType(contentType);

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
        return repository.findAll();
    }

    public ClassRecording getRecordingById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recording not found."));
    }

    public ClassRecording updateRecording(Long id, java.util.Map<String, Object> updates) {
        ClassRecording rec = getRecordingById(id);
        if (updates.containsKey("title") && updates.get("title") != null) {
            rec.setTitle(String.valueOf(updates.get("title")));
        }
        if (updates.containsKey("description") && updates.get("description") != null) {
            rec.setDescription(String.valueOf(updates.get("description")));
        }
        if (updates.containsKey("course") && updates.get("course") != null) {
            rec.setCourse(String.valueOf(updates.get("course")));
        }
        if (updates.containsKey("batch") && updates.get("batch") != null) {
            rec.setBatch(String.valueOf(updates.get("batch")));
        }
        if (updates.containsKey("videoUrl") && updates.get("videoUrl") != null) {
            String url = String.valueOf(updates.get("videoUrl")).trim();
            if (!url.isBlank()) {
                rec.setFileUrl(url);
                rec.setFileName("Online Video Lecture");
                rec.setFileType("video/mp4");
            }
        }
        return repository.save(rec);
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

    private boolean isAllowedVideo(String contentType, String fileName) {
        if (contentType != null && contentType.toLowerCase().startsWith("video/")) return true;
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv")
                    || lower.endsWith(".avi") || lower.endsWith(".webm") || lower.endsWith(".m4v")
                    || lower.endsWith(".3gp") || lower.endsWith(".wmv") || lower.endsWith(".ts")) {
                return true;
            }
        }
        return contentType == null || contentType.contains("octet-stream");
    }
}
