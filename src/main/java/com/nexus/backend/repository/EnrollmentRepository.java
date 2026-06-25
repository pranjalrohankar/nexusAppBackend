package com.nexus.backend.repository;

import com.nexus.backend.model.Enrollment;
import com.nexus.backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(Student student);
    boolean existsByStudentAndCourseTitle(Student student, String courseTitle);
    void deleteByStudent(Student student);
    int countByCourseTitle(String courseTitle);
    int countByCourseTitleIn(List<String> courseTitles);
    List<Enrollment> findByCourseTitle(String courseTitle);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM student_enrollments WHERE student_id = :studentId", nativeQuery = true)
    void deleteLegacyEnrollmentsByStudentId(@Param("studentId") Long studentId);
}
