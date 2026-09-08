package com.nexus.backend.service;

import com.nexus.backend.dto.BatchDto;
import com.nexus.backend.model.Batch;
import com.nexus.backend.repository.BatchRepository;
import com.nexus.backend.repository.EnrollmentRepository;
import com.nexus.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BatchService {

    private final BatchRepository batchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AppNotificationService appNotificationService;
    private final UserRepository userRepository;
    private final com.nexus.backend.repository.CourseRepository courseRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public BatchService(BatchRepository batchRepository, EnrollmentRepository enrollmentRepository,
                        AppNotificationService appNotificationService, UserRepository userRepository,
                        com.nexus.backend.repository.CourseRepository courseRepository,
                        org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.batchRepository = batchRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.appNotificationService = appNotificationService;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Batch createBatch(BatchDto request) {
        Batch batch = new Batch();
        batch.setBatchName(request.getBatchName());
        batch.setSelectCourse(request.getSelectCourse());
        batch.setInstructor(request.getInstructor());
        batch.setStartDate(request.getStartDate());
        batch.setEndDate(request.getEndDate());
        batch.setStatus(request.getStatus());
        batch.setClassDays(request.getClassDays());
        batch.setClassTimings(request.getClassTimings());
        batch.setDuration(request.getDuration());
        batch.setGoogleMeetLink(request.getGoogleMeetLink());
        if (request.getCoveredTopics() != null) {
            batch.setCoveredTopics(request.getCoveredTopics());
        }
        Batch saved = batchRepository.save(batch);
        String schedule = request.getClassDays() != null ? request.getClassDays().toString() : "schedule details";
        if (batch.getInstructor() != null && !batch.getInstructor().isBlank()) {
            userRepository.findAll().stream()
                .filter(u -> u.getName() != null && u.getName().equalsIgnoreCase(batch.getInstructor()))
                .map(u -> u.getEmail()).findFirst()
                .ifPresent(email -> appNotificationService.notifyScheduleUpdated(batch.getBatchName(), schedule, email));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllBatches() {
        try {
            List<Batch> batches = batchRepository.findAll();
            List<com.nexus.backend.model.Course> allCourses = courseRepository.findAll();
            return batches.stream().map(batch -> {
                Map<String, Object> batchMap = new HashMap<>();
                batchMap.put("id", batch.getId());
                batchMap.put("batchName", batch.getBatchName());
                batchMap.put("selectCourse", batch.getSelectCourse());
                batchMap.put("instructor", batch.getInstructor());
                batchMap.put("startDate", batch.getStartDate());
                batchMap.put("endDate", batch.getEndDate());
                batchMap.put("status", batch.getStatus());
                batchMap.put("createdAt", batch.getCreatedAt());
                batchMap.put("classDays", batch.getClassDays());

                String timings = batch.getClassTimings();
                String selectCourse = batch.getSelectCourse();
                if ((timings == null || timings.isBlank()) && selectCourse != null) {
                    timings = allCourses.stream()
                        .filter(c -> c != null && c.getTitle() != null && c.getTitle().equalsIgnoreCase(selectCourse.trim()))
                        .map(c -> c.getClassTimings())
                        .filter(t -> t != null && !t.isBlank())
                        .findFirst().orElse("");
                }
                batchMap.put("classTimings", timings != null ? timings : "");
                batchMap.put("courseTimings", timings != null ? timings : "");

                String duration = batch.getDuration();
                if ((duration == null || duration.isBlank()) && selectCourse != null) {
                    duration = allCourses.stream()
                        .filter(c -> c != null && c.getTitle() != null && c.getTitle().equalsIgnoreCase(selectCourse.trim()))
                        .map(c -> c.getDuration())
                        .filter(d -> d != null && !d.isBlank())
                        .findFirst().orElse("");
                }
                batchMap.put("duration", duration != null ? duration : "");

                String meetLink = batch.getGoogleMeetLink();
                if ((meetLink == null || meetLink.isBlank()) && selectCourse != null) {
                    meetLink = allCourses.stream()
                        .filter(c -> c != null && c.getTitle() != null && c.getTitle().equalsIgnoreCase(selectCourse.trim()))
                        .map(c -> c.getGoogleMeetLink() != null && !c.getGoogleMeetLink().isBlank() ? c.getGoogleMeetLink() : (c.getMeetLink() != null ? c.getMeetLink() : ""))
                        .filter(l -> l != null && !l.isBlank())
                        .findFirst().orElse("");
                }
                batchMap.put("googleMeetLink", meetLink != null ? meetLink : "");

                int studentCount = 0;
                if (selectCourse != null && !selectCourse.isBlank()) {
                    try {
                        studentCount = enrollmentRepository.countByCourseTitleIgnoreCase(selectCourse.trim());
                    } catch (Exception ignored) {}
                }
                batchMap.put("studentsCount", studentCount);

                String covered = batch.getCoveredTopics();
                if ((covered == null || covered.isBlank()) && selectCourse != null) {
                    final String cleanSelect = selectCourse.trim().toLowerCase();
                    covered = allCourses.stream()
                        .filter(c -> c != null && c.getTitle() != null && (
                            c.getTitle().trim().equalsIgnoreCase(selectCourse.trim()) ||
                            cleanSelect.contains(c.getTitle().trim().toLowerCase()) ||
                            c.getTitle().trim().toLowerCase().contains(cleanSelect)
                        ))
                        .map(c -> c.getCoveredTopics())
                        .filter(cv -> cv != null && !cv.isBlank())
                        .findFirst().orElse("");
                }
                batchMap.put("coveredTopics", covered != null ? covered : "");
                return batchMap;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    public Batch updateBatch(Long id, BatchDto request) {
        Batch batch = batchRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Batch not found"));
        batch.setBatchName(request.getBatchName());
        batch.setSelectCourse(request.getSelectCourse());
        batch.setInstructor(request.getInstructor());
        batch.setStartDate(request.getStartDate());
        batch.setEndDate(request.getEndDate());
        batch.setStatus(request.getStatus());
        batch.setClassDays(request.getClassDays());
        batch.setClassTimings(request.getClassTimings());
        batch.setDuration(request.getDuration());
        batch.setGoogleMeetLink(request.getGoogleMeetLink());
        if (request.getCoveredTopics() != null) {
            batch.setCoveredTopics(request.getCoveredTopics());
        }
        if (request.getGoogleMeetLink() != null && !request.getGoogleMeetLink().isBlank() && batch.getSelectCourse() != null) {
            courseRepository.findAll().stream()
                .filter(c -> c.getTitle() != null && c.getTitle().equalsIgnoreCase(batch.getSelectCourse().trim()))
                .forEach(c -> {
                    c.setGoogleMeetLink(request.getGoogleMeetLink());
                    courseRepository.save(c);
                });
        }
        Batch saved = batchRepository.save(batch);
        String schedule = request.getClassDays() != null ? request.getClassDays().toString() : "updated schedule";
        if (batch.getInstructor() != null && !batch.getInstructor().isBlank()) {
            userRepository.findAll().stream()
                .filter(u -> u.getName() != null && u.getName().equalsIgnoreCase(batch.getInstructor()))
                .map(u -> u.getEmail()).findFirst()
                .ifPresent(email -> appNotificationService.notifyScheduleUpdated(batch.getBatchName(), schedule, email));
        }
        return saved;
    }

    public Batch updateCoveredTopics(Long id, String coveredTopics) {
        if (id == null) return null;
        try {
            if (jdbcTemplate != null) {
                jdbcTemplate.update("UPDATE batches SET covered_topics = ? WHERE id = ?", coveredTopics, id);
            }
        } catch (Exception ignored) {}

        Optional<Batch> batchOpt = batchRepository.findById(id);
        if (batchOpt.isEmpty()) {
            return null;
        }
        Batch batch = batchOpt.get();
        batch.setCoveredTopics(coveredTopics);
        Batch saved = batchRepository.save(batch);

        // Synchronize to matching Course so all students & admins of this course see it immediately
        if (batch.getSelectCourse() != null && !batch.getSelectCourse().isBlank()) {
            final String cleanCourse = batch.getSelectCourse().trim().toLowerCase();
            try {
                if (jdbcTemplate != null) {
                    jdbcTemplate.update("UPDATE courses SET covered_topics = ? WHERE LOWER(title) = ?", coveredTopics, cleanCourse);
                }
            } catch (Exception ignored) {}
            try {
                courseRepository.findAll().stream()
                    .filter(c -> c.getTitle() != null && (
                        c.getTitle().trim().equalsIgnoreCase(batch.getSelectCourse().trim()) ||
                        cleanCourse.contains(c.getTitle().trim().toLowerCase()) ||
                        c.getTitle().trim().toLowerCase().contains(cleanCourse)
                    ))
                    .forEach(c -> {
                        c.setCoveredTopics(coveredTopics);
                        courseRepository.save(c);
                    });
            } catch (Exception ignored) {}
        }
        return saved;
    }

    public Batch getBatchById(Long id) {
        return batchRepository.findById(id).orElse(null);
    }

    public void deleteBatch(Long id) {
        batchRepository.deleteById(id);
    }
}
