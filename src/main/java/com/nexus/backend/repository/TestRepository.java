package com.nexus.backend.repository;

import com.nexus.backend.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByCourseTitleIgnoreCase(String courseTitle);
}
