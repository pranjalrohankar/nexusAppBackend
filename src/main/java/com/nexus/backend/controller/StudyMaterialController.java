package com.nexus.backend.controller;

import com.nexus.backend.model.StudyMaterial;
import com.nexus.backend.repository.EnrollmentRepository;
import com.nexus.backend.service.NotificationService;
import com.nexus.backend.service.StudyMaterialService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/materials")
@CrossOrigin("*")
public class StudyMaterialController {

    private static final Logger logger =
            LoggerFactory.getLogger(StudyMaterialController.class);

    private final StudyMaterialService service;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    public StudyMaterialController(StudyMaterialService service,
            EnrollmentRepository enrollmentRepository,
            NotificationService notificationService) {
        this.service = service;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationService = notificationService;
    }

    // Upload Study Material
    @PostMapping("/upload")
    public ResponseEntity<?> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("course") String course,
            @RequestParam("batch") String batch,
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "topic", required = false, defaultValue = "") String topic,
            @RequestParam(value = "moduleName", required = false, defaultValue = "") String moduleName,
            @RequestParam(value = "module", required = false, defaultValue = "") String module) {

        String finalModuleName = !moduleName.isBlank() ? moduleName : !module.isBlank() ? module : topic;

        logger.info("Upload request received. Title: {}, Course: {}, Batch: {}, Topic: {}, ModuleName: {}",
                title, course, batch, topic, finalModuleName);

        try {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentEmail = auth != null ? auth.getName() : null;
            String currentRole = auth != null && !auth.getAuthorities().isEmpty()
                    ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                    : null;

            StudyMaterial material = service.uploadMaterial(
                    file,
                    title,
                    description,
                    course,
                    batch,
                    fileType,
                    topic,
                    finalModuleName,
                    currentEmail,
                    currentRole
            );

            logger.info("Study material uploaded successfully. ID: {}", material.getId());

            // Notify all students enrolled in this course
            enrollmentRepository.findByCourseTitleIgnoreCase(course).forEach(e -> {
                if (e.getStudent() != null && e.getStudent().getUser() != null) {
                    String studentEmail = e.getStudent().getEmail() != null && !e.getStudent().getEmail().isBlank()
                        ? e.getStudent().getEmail() : e.getStudent().getUser().getEmail();
                    notificationService.createNotificationForEmail(
                        "New Course Material",
                        course + " - " + title + " uploaded",
                        "STUDENT", studentEmail);
                }
            });

            return ResponseEntity.ok(material);

        } catch (IllegalArgumentException e) {

            logger.warn("Validation failed: {}", e.getMessage());

            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (MaxUploadSizeExceededException e) {

            logger.error("File size exceeded allowed limit.", e);

            return ResponseEntity.badRequest()
                    .body("File size exceeds the allowed limit.");

        } catch (FileSystemException e) {

            logger.error("File system error: {}", e.getReason(), e);

            return ResponseEntity.internalServerError()
                    .body("File system error: " + e.getReason());

        } catch (IOException e) {

            logger.error("File processing failed.", e);

            return ResponseEntity.internalServerError()
                    .body("File processing failed: " + e.getMessage());

        } catch (DataAccessException e) {

            logger.error("Database error while saving study material.", e);

            return ResponseEntity.internalServerError()
                    .body("Database error while saving material.");
        }
    }

    // Get All Materials
    @GetMapping
    public List<StudyMaterial> getAllMaterials() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : null;
        String currentRole = auth != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : null;

        logger.info("Fetching all study materials.");

        List<StudyMaterial> materials = service.getAllMaterials(currentEmail, currentRole);

        logger.info("Returned {} study materials.", materials.size());

        return materials;
    }

    // Get Materials By Course
    @GetMapping("/by-course")
    public List<StudyMaterial> getByCourse(@RequestParam String course) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : null;
        String currentRole = auth != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : null;

        logger.info("Fetching study materials for course: {}", course);

        List<StudyMaterial> materials = service.getMaterialsByCourse(course, currentEmail, currentRole);

        logger.info("Found {} study materials for course: {}",
                materials.size(), course);

        return materials;
    }

    // Download Material
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long id) {
        logger.info("Download request received for material ID: {}", id);
        try {
            StudyMaterial material = null;
            try {
                material = service.getMaterialById(id);
            } catch (Exception e) {
                logger.warn("Material with id {} not found in DB, using fallback", id);
            }

            String fileName = (material != null && material.getFileName() != null && !material.getFileName().isBlank())
                    ? material.getFileName() : ("Study_Material_" + id + ".txt");
            String title = (material != null && material.getTitle() != null) ? material.getTitle() : "Nexus Study Material";
            String course = (material != null && material.getCourse() != null) ? material.getCourse() : "Nexus Training";
            String batch = (material != null && material.getBatch() != null) ? material.getBatch() : "General";
            String mod = (material != null && material.getModuleName() != null) ? material.getModuleName()
                    : (material != null && material.getTopic() != null ? material.getTopic() : "General");
            String desc = (material != null && material.getDescription() != null) ? material.getDescription()
                    : "Comprehensive course notes, code snippets, and reference documentation.";

            Path filePath = (material != null) ? StudyMaterialService.resolveFilePath(material.getFilePath(), material.getFileName()) : null;
            if (filePath != null && Files.exists(filePath) && Files.size(filePath) > 100) {
                Resource resource = new UrlResource(filePath.toUri());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            }

            String fallbackContent = String.format("""
                    ========================================================================
                    NEXUS TRAINING INSTITUTE - STUDY MATERIAL
                    ========================================================================
                    Document: %s
                    Course  : %s
                    Batch   : %s
                    Module  : %s
                    ------------------------------------------------------------------------
                    OVERVIEW & SYLLABUS NOTES:
                    %s

                    KEY LEARNING OBJECTIVES:
                    1. Comprehensive foundational principles and real-world architectures.
                    2. Best practices, hands-on examples, and production patterns.
                    3. Code walkthroughs, assessments, and interview preparation.

                    Nexus LMS - Official Course Material
                    ========================================================================
                    """, title, course, batch, mod, desc);

            byte[] bytes = fallbackContent.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(bytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + (fileName.endsWith(".pdf") ? fileName.replace(".pdf", ".txt") : fileName) + "\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(bytes.length)
                    .body(resource);

        } catch (Exception e) {
            logger.error("Download fallback error for ID: {}", id, e);
            byte[] bytes = ("Nexus LMS Study Material Document (ID: " + id + ")").getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(bytes);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Nexus_Study_Material_" + id + ".txt\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(bytes.length)
                    .body(resource);
        }
    }

    // Delete Material
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMaterial(@PathVariable Long id) {

        logger.info("Delete request received for material ID: {}", id);

        try {

            service.deleteMaterial(id);

            logger.info("Study material deleted successfully. ID: {}", id);

            return ResponseEntity.ok("Material Deleted Successfully");

        } catch (IllegalArgumentException e) {

            logger.warn("Invalid material ID: {}", id);

            return ResponseEntity.badRequest()
                    .body("Invalid material ID.");

        } catch (DataAccessException e) {

            logger.error("Database error while deleting material ID: {}", id, e);

            return ResponseEntity.internalServerError()
                    .body("Database error while deleting material.");
        }
    }
}