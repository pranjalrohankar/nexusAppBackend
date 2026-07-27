package com.nexus.backend.service;

import com.nexus.backend.model.Course;
import com.nexus.backend.repository.CourseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

@Service
@Transactional
public class CourseService {

    private final CourseRepository repository;

    public CourseService(CourseRepository repository) {
        this.repository = repository;
    }

    public Course createCourse(@NonNull Course course) {
        Assert.hasText(course.getTitle(), "Course title must not be blank");
        return repository.save(course);
    }

    public Course getCourseById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
    }

    @SuppressWarnings("null")
    public Page<Course> listCourses(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public List<Course> findByCategory(String category) {
        String cat = category == null ? null : category.toLowerCase();
        return repository.findAll().stream()
                .filter(c -> c.getCategory() != null && c.getCategory().toLowerCase().equals(cat))
                .toList();
    }

    public List<Course> findByStatus(Course.Status status) {
        return repository.findByStatus(status);
    }

    public List<Course> listAllCourses() {
        return repository.findAll();
    }

    public Course updateCourse(@NonNull Long id, @NonNull Course updatedCourse) {
        Assert.hasText(updatedCourse.getTitle(), "Course title must not be blank");
        Course existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));

        existing.setTitle(updatedCourse.getTitle());
        existing.setCategory(updatedCourse.getCategory());
        existing.setDescription(updatedCourse.getDescription());
        existing.setDuration(updatedCourse.getDuration());
        existing.setStartDate(updatedCourse.getStartDate());
        existing.setEndDate(updatedCourse.getEndDate());
        existing.setClassTimings(updatedCourse.getClassTimings());
        existing.setClassDays(updatedCourse.getClassDays());
        existing.setInstructor(updatedCourse.getInstructor());
        existing.setSyllabusTopics(updatedCourse.getSyllabusTopics());
        existing.setWhatYouWillLearn(updatedCourse.getWhatYouWillLearn());
        existing.setGoogleMeetLink(updatedCourse.getGoogleMeetLink());
        existing.setTotalSessions(updatedCourse.getTotalSessions());
        existing.setMaxCapacity(updatedCourse.getMaxCapacity());
        existing.setPrice(updatedCourse.getPrice());
        existing.setStatus(updatedCourse.getStatus());
        existing.setAutoGenerateMeetLink(updatedCourse.getAutoGenerateMeetLink());
        existing.setMeetLink(updatedCourse.getMeetLink());

        return repository.save(existing);
    }

    public Course saveCourse(@NonNull Course course) {
        return repository.save(course);
    }

    public void deleteCourse(@NonNull Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Course not found with id: " + id);
        }
        repository.deleteById(id);
    }
}