package com.nexus.backend.repository;

import com.nexus.backend.model.Student;
import com.nexus.backend.model.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findByStudent(Student student);
    List<TestAttempt> findByStudentId(Long studentId);
}
