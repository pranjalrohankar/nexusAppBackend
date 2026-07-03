package com.nexus.backend.repository;

import com.nexus.backend.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByInstructorIgnoreCase(String instructor);
}