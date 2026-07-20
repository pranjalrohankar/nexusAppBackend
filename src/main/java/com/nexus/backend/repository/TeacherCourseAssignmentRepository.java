package com.nexus.backend.repository;

import com.nexus.backend.model.Teacher;
import com.nexus.backend.model.TeacherCourseAssignment;
import com.nexus.backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TeacherCourseAssignmentRepository extends JpaRepository<TeacherCourseAssignment, Long> {
    
    List<TeacherCourseAssignment> findByTeacher(Teacher teacher);
    
    List<TeacherCourseAssignment> findByCourse(Course course);
    
    void deleteByTeacherAndCourse(Teacher teacher, Course course);
    
    boolean existsByTeacherAndCourse(Teacher teacher, Course course);
    
    @Query("SELECT COUNT(DISTINCT tca.course) FROM TeacherCourseAssignment tca WHERE tca.teacher = :teacher")
    Long countCoursesByTeacher(Teacher teacher);
}
