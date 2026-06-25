
package com.nexus.backend.controller;

import com.nexus.backend.model.StudyMaterial;
import com.nexus.backend.service.StudyMaterialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Handles all HTTP requests related to study materials
@RestController
@RequestMapping("/api/materials")
@CrossOrigin("*")
public class StudyMaterialController {

    private final StudyMaterialService service;

    public StudyMaterialController(StudyMaterialService service) {
        this.service = service;
    }

    // POST /api/materials/upload - Teacher uploads a file with metadata (title, course, batch, etc.)
    @PostMapping("/upload")
    public ResponseEntity<?> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("course") String course,
            @RequestParam("batch") String batch,
            @RequestParam("fileType") String fileType) {

        try {
            StudyMaterial material = service.uploadMaterial(
                    file,
                    title,
                    description,
                    course,
                    batch,
                    fileType
            );

            return ResponseEntity.ok(material); // return saved material on success

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // return error message on failure
        }
    }

    // GET /api/materials - Returns all uploaded study materials (used by admin/teacher)
    @GetMapping
    public List<StudyMaterial> getAllMaterials() {
        return service.getAllMaterials();
    }

    // GET /api/materials/by-course?course=... - Returns materials for a specific course (used by students)
    @GetMapping("/by-course")
    public List<StudyMaterial> getByCourse(@RequestParam String course) {
        return service.getMaterialsByCourse(course);
    }

    // DELETE /api/materials/{id} - Deletes a material by its ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMaterial(@PathVariable Long id) {
        service.deleteMaterial(id);
        return ResponseEntity.ok("Material Deleted Successfully");
    }
}