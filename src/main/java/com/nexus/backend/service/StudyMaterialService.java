package com.nexus.backend.service;

import com.nexus.backend.model.StudyMaterial;
import com.nexus.backend.repository.StudyMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

// Service class that handles business logic for study materials
@Service
public class StudyMaterialService {

    private final StudyMaterialRepository repository;

    public StudyMaterialService(StudyMaterialRepository repository) {
        this.repository = repository;
    }

    // Saves the uploaded file to disk and stores metadata in the database
    public StudyMaterial uploadMaterial(
            MultipartFile file,
            String title,
            String description,
            String course,
            String batch,
            String fileType) throws Exception {

        String uploadDir = "uploads/materials/"; // folder where files are saved on server
        Files.createDirectories(Paths.get(uploadDir)); // create folder if it doesn't exist

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename(); // unique filename
        Path filePath = Paths.get(uploadDir + fileName);
        Files.write(filePath, file.getBytes()); // write file bytes to disk

        // Build the entity and set all fields
        StudyMaterial material = new StudyMaterial();
        material.setTitle(title);
        material.setDescription(description);
        material.setCourse(course);
        material.setBatch(batch);
        material.setFileType(fileType);
        material.setFileName(fileName);
        material.setFilePath(filePath.toString());
        material.setUploadedAt(LocalDateTime.now()); // record upload time

        return repository.save(material); // save to DB and return
    }

    // Returns all study materials from the database
    public List<StudyMaterial> getAllMaterials() {
        return repository.findAll();
    }

    // Returns only materials matching the given course name (case-insensitive)
    public List<StudyMaterial> getMaterialsByCourse(String course) {
        return repository.findAll().stream()
                .filter(m -> course.equalsIgnoreCase(m.getCourse()))
                .toList();
    }

    // Deletes a material record from the database by ID
    public void deleteMaterial(Long id) {
        repository.deleteById(id);
    }
}
