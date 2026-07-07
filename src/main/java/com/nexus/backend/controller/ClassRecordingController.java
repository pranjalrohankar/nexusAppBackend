package com.nexus.backend.controller;

import com.nexus.backend.model.ClassRecording;
import com.nexus.backend.service.ClassRecordingService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recordings")
@CrossOrigin(origins = "*")
public class ClassRecordingController {

    private final ClassRecordingService recordingService;

    public ClassRecordingController(ClassRecordingService recordingService) {
        this.recordingService = recordingService;
    }

    // Upload Recording
    @PostMapping("/upload")
    public ResponseEntity<ClassRecording> uploadRecording(

            @RequestParam("file") MultipartFile file,

            @RequestParam("title") String title,

            @RequestParam(value = "description", required = false)
            String description,

            @RequestParam("classDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate classDate,

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
                file,
                title,
                description,
                classDate,
                duration,
                course,
                batch,
                currentEmail,
                currentRole
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

    // Delete Recording
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRecording(@PathVariable Long id) throws IOException {

        recordingService.deleteRecording(id);

        return ResponseEntity.ok("Recording deleted successfully.");
    }

}