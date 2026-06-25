package com.nexus.backend.repository;

import com.nexus.backend.model.Enrollment;
import com.nexus.backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(Student student);
    boolean existsByStudentAndCourseTitle(Student student, String courseTitle);
    void deleteByStudent(Student student);
}
