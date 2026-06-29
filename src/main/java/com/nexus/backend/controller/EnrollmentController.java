package com.nexus.backend.controller;

import com.nexus.backend.model.Enrollment;
import com.nexus.backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;

    @GetMapping("/course/{courseTitle}")
    public List<Enrollment> getEnrollmentsByCourse(@PathVariable String courseTitle) {
        return enrollmentRepository.findByCourseTitle(courseTitle);
    }

    @GetMapping("/count/course/{courseTitle}")
    public Map<String, Integer> getEnrollmentCount(@PathVariable String courseTitle) {
        return Map.of("count", enrollmentRepository.countByCourseTitle(courseTitle));
    }
}
