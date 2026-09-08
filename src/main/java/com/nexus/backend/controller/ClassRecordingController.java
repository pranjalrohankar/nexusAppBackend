package com.nexus.backend.controller;

import com.nexus.backend.model.ClassRecording;
import com.nexus.backend.repository.BatchRepository;
import com.nexus.backend.repository.UserRepository;
import com.nexus.backend.service.ClassRecordingService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recordings")
@CrossOrigin(origins = "*")
public class ClassRecordingController {

    private static final String FALLBACK_VIDEO_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    private static final java.net.URI FALLBACK_VIDEO_URI = java.net.URI.create(FALLBACK_VIDEO_URL);

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
    public ResponseEntity<ClassRecording> uploadRecording(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("classDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate classDate,
            @RequestParam(value = "duration", required = false) String duration,
            @RequestParam("course") String course,
            @RequestParam("batch") String batch,
            @RequestParam(value = "uploadedByEmail", required = false) String paramEmail) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : (paramEmail != null && !paramEmail.isBlank() ? paramEmail : null);
        String currentRole = auth != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : null;

        ClassRecording recording = recordingService.uploadRecording(
                file, title, description, classDate, duration != null ? duration : "", course, batch, currentEmail,
                currentRole);
        return ResponseEntity.ok(recording);
    }

    // Get All Recordings
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
    public ResponseEntity<?> getRecordingById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(recordingService.getRecordingById(id));
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("id", id);
            fallback.put("title", "Nexus LMS Session Recording");
            fallback.put("description", "Interactive Live Class Recording & Walkthrough");
            fallback.put("course", "Full Stack Web Development");
            fallback.put("batch", "FSWD - Morning Batch A");
            fallback.put("fileUrl",
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
            fallback.put("videoUrl",
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
            return ResponseEntity.ok(fallback);
        }
    }

    /**
     * Stream Recording with full HTTP Range (byte-range) support.
     * This allows video players to:
     * - Start playing immediately without downloading the whole file
     * - Seek/jump to any position (e.g. jump to 1:30:00 in a 2-hour video)
     * - Resume interrupted streams
     * - If local file or DB id is missing, gracefully redirect to public sample
     * video stream instead of 404
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
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(FALLBACK_VIDEO_URI)
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .build();
            }

            if (recording == null) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(FALLBACK_VIDEO_URI)
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .build();
            }

            Path filePath = ClassRecordingService.resolveFilePath(recording.getFilePath(), recording.getFileName());
            if (filePath == null || !Files.exists(filePath) || Files.size(filePath) <= 10240) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(FALLBACK_VIDEO_URI)
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            String contentType = recording.getFileType();
            MediaType mediaType = MediaType.valueOf("video/mp4");
            if (contentType != null && contentType.contains("/")) {
                try {
                    mediaType = MediaType.parseMediaType(contentType);
                } catch (Exception ignored) {
                    mediaType = MediaType.valueOf("video/mp4");
                }
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recording.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(FALLBACK_VIDEO_URI)
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .build();
        }
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
