package com.nexus.backend.service;

import com.nexus.backend.model.StudyMaterial;
import com.nexus.backend.repository.StudyMaterialRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StudyMaterialService {

    private static final Logger logger = LoggerFactory.getLogger(StudyMaterialService.class);

    private final StudyMaterialRepository repository;

    public StudyMaterialService(StudyMaterialRepository repository) {
        this.repository = repository;
    }

    // Upload Study Material
    public StudyMaterial uploadMaterial(
            MultipartFile file,
            String title,
            String description,
            String course,
            String batch,
            String fileType,
            String uploadedByEmail,
            String uploadedByRole) throws IOException {

        logger.info("Uploading study material. Title: {}, Course: {}, Batch: {}",
                title, course, batch);

        if (file == null || file.isEmpty()) {
            logger.warn("Upload failed: uploaded file is missing or empty.");
            throw new IllegalArgumentException("Uploaded file is required.");
        }

        String uploadDir = "uploads/materials/";

        Files.createDirectories(Paths.get(uploadDir));

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        Path filePath = Paths.get(uploadDir + fileName);

        Files.write(filePath, file.getBytes());

        logger.info("File saved successfully at: {}", filePath);

        StudyMaterial material = new StudyMaterial();
        material.setTitle(title);
        material.setDescription(description);
        material.setCourse(course);
        material.setBatch(batch);
        material.setFileType(fileType);
        material.setFileName(fileName);
        material.setFilePath(filePath.toAbsolutePath().toString());
        material.setUploadedByEmail(uploadedByEmail);
        material.setUploadedByRole(uploadedByRole);
        material.setUploadedAt(LocalDateTime.now());

        StudyMaterial savedMaterial = repository.save(material);
        savedMaterial.setFileUrl("/api/materials/download/" + savedMaterial.getId());
        savedMaterial = repository.save(savedMaterial);

        logger.info("Study material saved successfully with ID: {}",
                savedMaterial.getId());

        return savedMaterial;
    }

    // Get All Study Materials
    public List<StudyMaterial> getAllMaterials(String currentEmail, String currentRole) {

        logger.info("Fetching study materials for user: {} with role: {}", currentEmail, currentRole);

        List<StudyMaterial> materials = repository.findAll();

        if (currentEmail == null || currentEmail.isBlank()) {
            logger.info("No authenticated user found. Returning all materials.");
            return materials;
        }

        if ("ADMIN".equalsIgnoreCase(currentRole)) {
            logger.info("Admin request. Returning all materials.");
            return materials;
        }

        List<StudyMaterial> filteredMaterials = materials.stream()
                .filter(m -> currentEmail.equalsIgnoreCase(m.getUploadedByEmail()))
                .toList();

        logger.info("Total study materials found for user {}: {}", currentEmail, filteredMaterials.size());

        return filteredMaterials;
    }

    // Get Study Materials By Course
    public List<StudyMaterial> getMaterialsByCourse(String course, String currentEmail, String currentRole) {

        logger.info("Fetching study materials for course: {} by user: {} with role: {}", course, currentEmail, currentRole);

        List<StudyMaterial> materials = repository.findAll()
                .stream()
                .filter(m -> course.equalsIgnoreCase(m.getCourse()))
                .toList();

        if (currentEmail == null || currentEmail.isBlank()) {
            logger.info("No authenticated user found. Returning materials for course {}.", course);
            return materials;
        }

        if ("ADMIN".equalsIgnoreCase(currentRole)) {
            logger.info("Admin request. Returning all materials for course {}.", course);
            return materials;
        }

        List<StudyMaterial> filteredMaterials = materials.stream()
                .filter(m -> currentEmail.equalsIgnoreCase(m.getUploadedByEmail()))
                .toList();

        logger.info("Found {} study materials for course: {} for user {}",
                filteredMaterials.size(), course, currentEmail);

        return filteredMaterials;
    }

    public StudyMaterial getMaterialById(Long id) {
        logger.info("Fetching study material by ID: {}", id);

        if (id == null) {
            logger.warn("Material ID is null.");
            throw new IllegalArgumentException("Material ID must not be null.");
        }

        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Material not found."));
    }

    // Delete Study Material
    public void deleteMaterial(Long id) {

        logger.info("Deleting study material with ID: {}", id);

        if (id == null) {

            logger.warn("Delete operation failed. Material ID is null.");

            throw new IllegalArgumentException("Material ID must not be null.");
        }

        repository.deleteById(id);

        logger.info("Study material deleted successfully. ID: {}", id);
    }
}