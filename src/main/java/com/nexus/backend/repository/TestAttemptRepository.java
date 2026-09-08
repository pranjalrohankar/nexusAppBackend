package com.nexus.backend.repository;

import com.nexus.backend.model.Student;
import com.nexus.backend.model.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findByStudent(Student student);

    @Query("SELECT a FROM TestAttempt a WHERE a.student.id = :studentId")
    List<TestAttempt> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT a FROM TestAttempt a WHERE a.test.id = :testId")
    List<TestAttempt> findByTestId(@Param("testId") Long testId);

    List<TestAttempt> findByStatus(String status);

    List<TestAttempt> findAllByOrderBySubmittedAtDesc();

    @Query("SELECT a FROM TestAttempt a WHERE a.student.id = :studentId AND a.test.id = :testId")
    Optional<TestAttempt> findByStudentIdAndTestId(@Param("studentId") Long studentId, @Param("testId") Long testId);

    List<TestAttempt> findByStudentEmailIgnoreCaseOrderBySubmittedAtDesc(String studentEmail);
}
