package com.nexus.backend.controller;

import com.nexus.backend.model.ClassRecording;
import com.nexus.backend.repository.BatchRepository;
import com.nexus.backend.repository.UserRepository;
import com.nexus.backend.service.ClassRecordingService;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recordings")
public class ClassRecordingController {

    private final ClassRecordingService recordingService;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;

    public ClassRecordingController(ClassRecordingService recordingService, UserRepository userRepository,
            BatchRepository batchRepository) {
        this.recordingService = recordingService;
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
    }

    // Upload Recording
    @PostMapping("/upload")
    public ResponseEntity<?> uploadRecording(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "classDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate classDate,
            @RequestParam(value = "duration", required = false) String duration,
            @RequestParam(value = "course", required = false) String course,
            @RequestParam(value = "batch", required = false) String batch,
            @RequestParam(value = "uploadedByEmail", required = false) String paramEmail) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentEmail = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())
                    ? auth.getName()
                    : (paramEmail != null && !paramEmail.isBlank() ? paramEmail : null);
            String currentRole = auth != null && !auth.getAuthorities().isEmpty()
                    ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                    : "ANONYMOUS";

            String safeTitle = (title != null && !title.isBlank()) ? title.trim() : "Class Session";
            LocalDate safeDate = classDate != null ? classDate : LocalDate.now();

            ClassRecording recording = recordingService.uploadRecording(
                    file, videoUrl, safeTitle, description, safeDate, duration, course, batch, currentEmail, currentRole);
            return ResponseEntity.ok(recording);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Upload error: " + e.getMessage()));
        }
    }

    // Get All Recordings (Filtered by Teacher role / student access)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRecordings() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : null;
        String currentRole = auth != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : null;

        List<ClassRecording> list = recordingService.getAllRecordings(currentEmail, currentRole);

        // Build user map for fast lookup of uploader names by email
        Map<String, String> userNames = new HashMap<>();
        userRepository.findAll().forEach(u -> {
            if (u.getEmail() != null && u.getName() != null) {
                userNames.put(u.getEmail().toLowerCase().trim(), u.getName());
            }
        });

        // Build instructor name map: course -> instructor name from batch
        Map<String, String> courseInstructor = new HashMap<>();
        batchRepository.findAll().forEach(b -> {
            if (b.getSelectCourse() != null && b.getInstructor() != null) {
                courseInstructor.putIfAbsent(b.getSelectCourse().toLowerCase(), b.getInstructor());
            }
        });

        List<Map<String, Object>> result = list.stream().map(r -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", r.getId());
            dto.put("title", r.getTitle());
            dto.put("description", r.getDescription());
            dto.put("classDate", r.getClassDate() != null ? r.getClassDate().toString() : null);
            dto.put("duration", r.getDuration());
            dto.put("course", r.getCourse());
            dto.put("batch", r.getBatch());
            dto.put("fileName", r.getFileName());
            dto.put("filePath", r.getFilePath());
            String streamUrl = (r.getFileUrl() != null && !r.getFileUrl().isBlank()) ? r.getFileUrl()
                    : ("/api/recordings/stream/" + r.getId());
            dto.put("fileUrl", streamUrl);
            dto.put("videoUrl", streamUrl);
            dto.put("fileSize", r.getFileSize());
            dto.put("fileType", r.getFileType());
            dto.put("uploadedByEmail", r.getUploadedByEmail());
            dto.put("uploadedByRole", r.getUploadedByRole());
            dto.put("uploadedAt", r.getUploadedAt() != null ? r.getUploadedAt().toString() : null);

            // Resolve actual uploader name from User table first
            String instructor = null;
            if (r.getUploadedByEmail() != null) {
                instructor = userNames.get(r.getUploadedByEmail().toLowerCase().trim());
            }
            if (instructor == null || instructor.trim().isEmpty()) {
                if (r.getUploadedByEmail() != null) {
                    String prefix = r.getUploadedByEmail().split("@")[0].replace(".", " ");
                    instructor = Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
                }
            }
            if (instructor == null || instructor.trim().isEmpty()) {
                instructor = courseInstructor.get(r.getCourse() != null ? r.getCourse().toLowerCase() : "");
            }
            dto.put("instructor", instructor != null ? instructor : "Instructor");
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // Get Recording By ID
    @GetMapping("/{id}")
    public ResponseEntity<ClassRecording> getRecordingById(@PathVariable Long id) {
        ClassRecording recording = recordingService.getRecordingById(id);
        if (recording == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(recording);
    }

    // Get Batch Statistics for Teacher
    @GetMapping("/batch-stats")
    public ResponseEntity<Map<String, Object>> getBatchStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : null;
        String currentRole = auth != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : null;

        List<ClassRecording> list = recordingService.getAllRecordings(currentEmail, currentRole);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecordings", list.size());

        // Group by batch
        Map<String, Long> byBatch = list.stream()
                .collect(Collectors.groupingBy(ClassRecording::getBatch, Collectors.counting()));
        stats.put("recordingsByBatch", byBatch);

        // Group by course
        Map<String, Long> byCourse = list.stream()
                .collect(Collectors.groupingBy(ClassRecording::getCourse, Collectors.counting()));
        stats.put("recordingsByCourse", byCourse);

        // Active batches count
        long activeBatchesCount = batchRepository.findAll().stream()
                .filter(b -> b.getStatus() != null && "Active".equalsIgnoreCase(b.getStatus().name()))
                .count();
        stats.put("activeBatchesCount", activeBatchesCount);

        // Total students across all batches
        long totalStudents = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "STUDENT".equalsIgnoreCase(u.getRole().name()))
                .count();
        stats.put("totalStudents", totalStudents);

        return ResponseEntity.ok(stats);
    }

    // Get Recordings by Batch Name
    @GetMapping("/batch/{batchName}")
    public ResponseEntity<List<ClassRecording>> getRecordingsByBatch(@PathVariable String batchName) {
        List<ClassRecording> list = recordingService.getAllRecordings(null, null).stream()
                .filter(r -> r.getBatch() != null && r.getBatch().equalsIgnoreCase(batchName))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // Get Recordings by Course Name
    @GetMapping("/course/{courseName}")
    public ResponseEntity<List<ClassRecording>> getRecordingsByCourse(@PathVariable String courseName) {
        List<ClassRecording> list = recordingService.getAllRecordings(null, null).stream()
                .filter(r -> r.getCourse() != null && r.getCourse().equalsIgnoreCase(courseName))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /**
     * Resilient stream endpoint with HTTP 206 Partial Content / Range requests.
     * Features:
     * - Direct redirection for external video links (YouTube, Drive, Vimeo, CDN)
     * - Self-healing disk cache: Restores video bytes from PostgreSQL (bytea) if Render container woke up from sleep
     * - HTTP 206 Range-based streaming allowing players to seek/jump to any point instantly
     */
    @GetMapping("/stream/{id}")
    public ResponseEntity<?> streamRecording(
            @PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            ClassRecording recording = null;
            try {
                recording = recordingService.getRecordingById(id);
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Recording not found."));
            }

            if (recording == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Recording not found."));
            }

            // 1. External URL redirection (YouTube, Google Drive, Vimeo, CDN)
            if (recording.getFileUrl() != null && recording.getFileUrl().startsWith("http") && !recording.getFileUrl().contains("/api/recordings/stream/")) {
                try {
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(java.net.URI.create(recording.getFileUrl()))
                            .build();
                } catch (Exception ignored) {}
            }

            // 2. Check local disk cache
            Path filePath = ClassRecordingService.resolveFilePath(recording.getFilePath(), recording.getFileName());

            // 3. Self-healing disk cache: if disk file was wiped by container sleep, restore from PostgreSQL
            if ((filePath == null || !Files.exists(filePath) || Files.size(filePath) == 0) && recording.getFileData() != null && recording.getFileData().length > 0) {
                try {
                    File uploadDir = new File("uploads/recordings");
                    if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                        uploadDir = new File(System.getProperty("java.io.tmpdir", "/tmp"), "uploads/recordings");
                        uploadDir.mkdirs();
                    }
                    String safeName = (recording.getFileName() != null && !recording.getFileName().isBlank())
                            ? recording.getFileName()
                            : ("rec_" + recording.getId() + ".mp4");
                    File restoredFile = new File(uploadDir, safeName);
                    Files.write(restoredFile.toPath(), recording.getFileData(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    filePath = restoredFile.toPath();
                } catch (Exception restoreEx) {
                    // Fall back to direct ByteArrayResource if disk write fails
                }
            }

            Resource resource = null;
            long contentLength = 0;
            if (filePath != null && Files.exists(filePath) && Files.size(filePath) > 0) {
                resource = new UrlResource(filePath.toUri());
                contentLength = Files.size(filePath);
            } else if (recording.getFileData() != null && recording.getFileData().length > 0) {
                resource = new ByteArrayResource(recording.getFileData());
                contentLength = recording.getFileData().length;
            }

            if (resource == null || contentLength == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Video data is not available for this recording. Please re-upload."));
            }

            String contentType = recording.getFileType();
            MediaType mediaType = MediaType.valueOf("video/mp4");
            if (contentType != null && contentType.contains("/")) {
                try {
                    mediaType = MediaType.parseMediaType(contentType);
                } catch (Exception ignored) {
                    mediaType = MediaType.valueOf("video/mp4");
                }
            }

            // HTTP 206 Partial Content for video seeking and smooth playback
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                try {
                    List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
                    if (!ranges.isEmpty()) {
                        HttpRange range = ranges.get(0);
                        long start = range.getRangeStart(contentLength);
                        long end = range.getRangeEnd(contentLength);
                        long rangeLength = Math.min(1024 * 1024 * 5, end - start + 1); // 5MB chunk
                        ResourceRegion region = new ResourceRegion(resource, start, rangeLength);
                        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                                .contentType(mediaType)
                                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                                .body(region);
                    }
                } catch (Exception ignored) {}
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (recording.getFileName() != null ? recording.getFileName() : "recording.mp4") + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error streaming recording: " + e.getMessage()));
        }
    }

    // Update Recording
    @PutMapping("/{id}")
    public ResponseEntity<ClassRecording> updateRecording(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        ClassRecording updated = recordingService.updateRecording(id, updates);
        return ResponseEntity.ok(updated);
    }

    // Delete Recording
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRecording(@PathVariable Long id) throws IOException {
        recordingService.deleteRecording(id);
        return ResponseEntity.ok("Recording deleted successfully.");
    }

    // Delete ALL recordings (cleanup orphan DB records)
    @DeleteMapping("/all")
    public ResponseEntity<String> deleteAllRecordings() {
        try {
            recordingService.deleteAllRecordings();
            return ResponseEntity.ok("All recordings deleted.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
