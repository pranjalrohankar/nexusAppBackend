package com.nexus.backend.repository;

import com.nexus.backend.model.Student;
import com.nexus.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUser(User user);
    Optional<Student> findByEmail(String email);
    List<Student> findByCourse(String course);
    long countByCourse(String course);
}
