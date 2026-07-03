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
            String fileType) throws IOException {

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
        material.setUploadedAt(LocalDateTime.now());

        StudyMaterial savedMaterial = repository.save(material);
        savedMaterial.setFileUrl("/api/materials/download/" + savedMaterial.getId());
        savedMaterial = repository.save(savedMaterial);

        logger.info("Study material saved successfully with ID: {}",
                savedMaterial.getId());

        return savedMaterial;
    }

    // Get All Study Materials
    public List<StudyMaterial> getAllMaterials() {

        logger.info("Fetching all study materials.");

        List<StudyMaterial> materials = repository.findAll();

        logger.info("Total study materials found: {}", materials.size());

        return materials;
    }

    // Get Study Materials By Course
    public List<StudyMaterial> getMaterialsByCourse(String course) {

        logger.info("Fetching study materials for course: {}", course);

        List<StudyMaterial> materials = repository.findAll()
                .stream()
                .filter(m -> course.equalsIgnoreCase(m.getCourse()))
                .toList();

        logger.info("Found {} study materials for course: {}",
                materials.size(), course);

        return materials;
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