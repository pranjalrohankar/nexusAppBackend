package com.nexus.backend.repository;

import com.nexus.backend.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByCourseTitleIgnoreCase(String courseTitle);
    List<Test> findByCategoryIgnoreCase(String category);
    List<Test> findByCreatedByTeacherEmailIgnoreCase(String email);
    List<Test> findAllByOrderByCreatedAtDesc();
}
