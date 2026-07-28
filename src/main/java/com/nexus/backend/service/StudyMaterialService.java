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
            String topic,
            String uploadedByEmail,
            String uploadedByRole) throws IOException {

        logger.info("Uploading study material. Title: {}, Course: {}, Batch: {}, Topic: {}",
                title, course, batch, topic);

        if (file == null || file.isEmpty()) {
            logger.warn("Upload failed: uploaded file is missing or empty.");
            throw new IllegalArgumentException("Uploaded file is required.");
        }

        String uploadDir = "uploads/materials";
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        String safeFileName = (file.getOriginalFilename() != null ? file.getOriginalFilename() : "material")
                .replaceAll("\\s+", "_");
        String fileName = System.currentTimeMillis() + "_" + safeFileName;

        Path filePath = uploadPath.resolve(fileName);

        Files.write(filePath, file.getBytes());

        logger.info("File saved successfully at: {}", filePath);

        StudyMaterial material = new StudyMaterial();
        material.setTitle(title);
        material.setDescription(description);
        material.setCourse(course);
        material.setBatch(batch);
        material.setTopic(topic != null ? topic.trim() : "");
        material.setFileType(fileType);
        material.setFileName(fileName);
        material.setFilePath("uploads/materials/" + fileName);
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

    public static Path resolveFilePath(String storedFilePath, String fileName) {
        if (fileName != null && !fileName.isBlank()) {
            Path p1 = Paths.get("uploads", "materials", fileName);
            if (Files.exists(p1)) return p1;
        }
        if (storedFilePath != null && !storedFilePath.isBlank()) {
            Path p2 = Paths.get(storedFilePath);
            if (Files.exists(p2)) return p2;

            Path nameOnly = p2.getFileName();
            if (nameOnly != null) {
                Path p3 = Paths.get("uploads", "materials", nameOnly.toString());
                if (Files.exists(p3)) return p3;
            }
        }
        return Paths.get("uploads", "materials", fileName != null ? fileName : "");
    }

    // Get All Study Materials
    public List<StudyMaterial> getAllMaterials(String currentEmail, String currentRole) {
        logger.info("Fetching study materials for user: {} with role: {}", currentEmail, currentRole);
        List<StudyMaterial> materials = repository.findAll();
        return materials;
    }

    // Get Study Materials By Course
    public List<StudyMaterial> getMaterialsByCourse(String course, String currentEmail, String currentRole) {
        logger.info("Fetching study materials for course: {} by user: {} with role: {}", course, currentEmail, currentRole);
        List<StudyMaterial> materials = repository.findAll()
                .stream()
                .filter(m -> course.equalsIgnoreCase(m.getCourse()))
                .toList();
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

        StudyMaterial material = repository.findById(id).orElse(null);
        if (material != null) {
            Path path = resolveFilePath(material.getFilePath(), material.getFileName());
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                logger.warn("Could not delete file from disk: {}", path, e);
            }
        }

        repository.deleteById(id);

        logger.info("Study material deleted successfully. ID: {}", id);
    }
}