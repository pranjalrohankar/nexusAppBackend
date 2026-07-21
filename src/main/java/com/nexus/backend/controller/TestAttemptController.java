package com.nexus.backend.controller;

import com.nexus.backend.model.Student;
import com.nexus.backend.model.TestAttempt;
import com.nexus.backend.repository.StudentRepository;
import com.nexus.backend.repository.TestAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test-attempts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TestAttemptController {

    private final TestAttemptRepository testAttemptRepository;
    private final StudentRepository studentRepository;

    /**
     * GET /api/test-attempts/student/{studentId}
     * Returns all test attempts for a given student, enriched with test details.
     */
    @GetMapping("/student/{studentId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAttemptsByStudent(
            @PathVariable Long studentId) {

        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<TestAttempt> attempts = testAttemptRepository.findByStudent(studentOpt.get());

        List<Map<String, Object>> result = attempts.stream().map(attempt -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", attempt.getId());
            dto.put("marks", attempt.getMarksObtained());
            dto.put("date", attempt.getAttemptDate() != null ? attempt.getAttemptDate() : "");
            dto.put("time", attempt.getAttemptTime() != null ? attempt.getAttemptTime() : "");

            if (attempt.getTest() != null) {
                dto.put("testName", attempt.getTest().getTestName());
                dto.put("subject", attempt.getTest().getTestName());
                dto.put("totalMarks", attempt.getTest().getTotalMarks());
                dto.put("courseTitle", attempt.getTest().getCourseTitle());
            } else {
                dto.put("testName", "Test");
                dto.put("subject", "Test");
                dto.put("totalMarks", null);
                dto.put("courseTitle", "");
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
