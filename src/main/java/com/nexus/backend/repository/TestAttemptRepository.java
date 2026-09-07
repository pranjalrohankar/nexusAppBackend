package com.nexus.backend.repository;

import com.nexus.backend.model.Student;
import com.nexus.backend.model.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findByStudent(Student student);
    List<TestAttempt> findByStudentId(Long studentId);
    List<TestAttempt> findByTestId(Long testId);
    List<TestAttempt> findByStatus(String status);
    List<TestAttempt> findAllByOrderBySubmittedAtDesc();
    Optional<TestAttempt> findByStudentIdAndTestId(Long studentId, Long testId);
    List<TestAttempt> findByStudentEmailIgnoreCaseOrderBySubmittedAtDesc(String studentEmail);
}
