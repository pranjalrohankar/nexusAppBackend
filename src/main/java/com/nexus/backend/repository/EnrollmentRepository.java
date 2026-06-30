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

    // Case-insensitive fetch with Student AND User joined eagerly
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student s LEFT JOIN FETCH s.user WHERE LOWER(TRIM(e.courseTitle)) = LOWER(TRIM(:courseTitle))")
    List<Enrollment> findByCourseTitleIgnoreCase(@Param("courseTitle") String courseTitle);

    // Count with case-insensitive match
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE LOWER(TRIM(e.courseTitle)) = LOWER(TRIM(:courseTitle))")
    int countByCourseTitleIgnoreCase(@Param("courseTitle") String courseTitle);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM student_enrollments WHERE student_id = :studentId", nativeQuery = true)
    void deleteLegacyEnrollmentsByStudentId(@Param("studentId") Long studentId);
}
