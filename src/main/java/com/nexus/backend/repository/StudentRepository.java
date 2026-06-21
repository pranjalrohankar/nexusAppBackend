package com.nexus.backend.repository;

import com.nexus.backend.model.Student;
import com.nexus.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUser(User user);
    List<Student> findAll();
}
