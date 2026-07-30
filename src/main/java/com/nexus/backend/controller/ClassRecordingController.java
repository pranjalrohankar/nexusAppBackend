package com.nexus.backend.controller;

import com.nexus.backend.model.ClassRecording;
import com.nexus.backend.service.ClassRecordingService;


import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recordings")
@CrossOrigin(origins = "*")
public class ClassRecordingController {

    private static final int CHUNK_SIZE = 1024 * 1024; // 1 MB chunks

    private final ClassRecordingService recordingService;

    public ClassRecordingController(ClassRecordingService recordingService) {
        this.recordingService = recordingService;
    }

    // Upload Recording
    @PostMapping("/upload")
    public ResponseEntity<ClassRecording> uploadRecording(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("classDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate classDate,
            @RequestParam("duration") String duration,
            @RequestParam("course") String course,
            @RequestParam("batch") String batch
    ) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : null;
        String currentRole = auth != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : null;

        ClassRecording recording = recordingService.uploadRecording(
                file, title, description, classDate, duration, course, batch, currentEmail, currentRole
        );
        return ResponseEntity.ok(recording);
    }

    // Get All Recordings
    @GetMapping
    public ResponseEntity<List<ClassRecording>> getAllRecordings() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : null;
        String currentRole = auth != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : null;
        return ResponseEntity.ok(recordingService.getAllRecordings(currentEmail, currentRole));
    }

    // Get Recording By ID
    @GetMapping("/{id}")
    public ResponseEntity<ClassRecording> getRecordingById(@PathVariable Long id) {
        return ResponseEntity.ok(recordingService.getRecordingById(id));
    }

    /**
     * Stream Recording with full HTTP Range (byte-range) support.
     * This allows video players to:
     *  - Start playing immediately without downloading the whole file
     *  - Seek/jump to any position (e.g. jump to 1:30:00 in a 2-hour video)
     *  - Resume interrupted streams
     */
    @GetMapping("/stream/{id}")
    public ResponseEntity<StreamingResponseBody> streamRecording(
            @PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        try {
            ClassRecording recording = recordingService.getRecordingById(id);

            Path filePath = ClassRecordingService.resolveFilePath(recording.getFilePath(), recording.getFileName());
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            long fileSize = Files.size(filePath);
            String contentType = recording.getFileType() != null ? recording.getFileType() : "video/mp4";

            // No Range header — send full file (still streamed, not loaded into RAM)
            if (rangeHeader == null || rangeHeader.isEmpty()) {
                StreamingResponseBody body = out -> {
                    try (InputStream in = Files.newInputStream(filePath)) {
                        byte[] buf = new byte[CHUNK_SIZE];
                        int read;
                        while ((read = in.read(buf)) != -1) {
                            out.write(buf, 0, read);
                            out.flush();
                        }
                    }
                };
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recording.getFileName() + "\"")
                        .body(body);
            }

            // Parse Range header: "bytes=start-end"
            String rangeValue = rangeHeader.replace("bytes=", "");
            String[] parts = rangeValue.split("-");
            long start = Long.parseLong(parts[0].trim());
            long end = parts.length > 1 && !parts[1].trim().isEmpty()
                    ? Long.parseLong(parts[1].trim())
                    : Math.min(start + CHUNK_SIZE - 1, fileSize - 1);

            // Clamp to file bounds
            end = Math.min(end, fileSize - 1);
            long contentLength = end - start + 1;

            final long rangeStart = start;
            final long rangeEnd = end;

            StreamingResponseBody body = out -> {
                try (InputStream in = Files.newInputStream(filePath)) {
                    long skipped = in.skip(rangeStart);
                    if (skipped < rangeStart) return;
                    byte[] buf = new byte[CHUNK_SIZE];
                    long remaining = rangeEnd - rangeStart + 1;
                    int read;
                    while (remaining > 0 && (read = in.read(buf, 0, (int) Math.min(buf.length, remaining))) != -1) {
                        out.write(buf, 0, read);
                        out.flush();
                        remaining -= read;
                    }
                }
            };

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recording.getFileName() + "\"")
                    .body(body);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
