package com.nexus.backend.repository;

import com.nexus.backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByCategoryIgnoreCase(String category);
    List<Course> findByStatus(Course.Status status);
}