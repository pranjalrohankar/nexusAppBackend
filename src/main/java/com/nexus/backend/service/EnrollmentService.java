package com.nexus.backend.service;

import com.nexus.backend.model.Enrollment;
import com.nexus.backend.model.Student;
import com.nexus.backend.repository.EnrollmentRepository;
import com.nexus.backend.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public Enrollment enrollStudent(Long studentId, String courseTitle, String paymentStatus) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        if (enrollmentRepository.existsByStudentAndCourseTitle(student, courseTitle)) {
            throw new IllegalStateException("Student is already enrolled in course: " + courseTitle);
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourseTitle(courseTitle);
        enrollment.setEnrollmentDate(LocalDate.now().toString());
        enrollment.setPaymentStatus(paymentStatus != null ? paymentStatus : "PAID");

        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsByStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));
        return enrollmentRepository.findByStudent(student);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsByCourse(String courseTitle) {
        return enrollmentRepository.findByCourseTitleIgnoreCase(courseTitle);
    }

    @Transactional(readOnly = true)
    public int getEnrollmentCountByCourse(String courseTitle) {
        return enrollmentRepository.countByCourseTitleIgnoreCase(courseTitle);
    }

    @Transactional
    public void unenrollStudent(Long studentId, String courseTitle) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));
        List<Enrollment> list = enrollmentRepository.findByStudent(student);
        list.stream()
                .filter(e -> e.getCourseTitle().equalsIgnoreCase(courseTitle))
                .findFirst()
                .ifPresent(enrollmentRepository::delete);
    }
}
