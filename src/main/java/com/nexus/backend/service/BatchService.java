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
import java.util.stream.Collectors;

@Service
@Transactional
public class BatchService {

    private final BatchRepository batchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AppNotificationService appNotificationService;
    private final UserRepository userRepository;

    public BatchService(BatchRepository batchRepository, EnrollmentRepository enrollmentRepository,
                        AppNotificationService appNotificationService, UserRepository userRepository) {
        this.batchRepository = batchRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.appNotificationService = appNotificationService;
        this.userRepository = userRepository;
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
        return batchRepository.save(batch);
    }

    public List<Map<String, Object>> getAllBatches() {
        List<Batch> batches = batchRepository.findAll();
        return batches.stream().map(batch -> {
            Map<String, Object> batchMap = new HashMap<>();
            batchMap.put("id", batch.getId());
            batchMap.put("batchName", batch.getBatchName());
            batchMap.put("selectCourse", batch.getSelectCourse());
            batchMap.put("instructor", batch.getInstructor());
            batchMap.put("startDate", batch.getStartDate());
            batchMap.put("endDate", batch.getEndDate());
            batchMap.put("classDays", batch.getClassDays());
            batchMap.put("status", batch.getStatus());
            batchMap.put("createdAt", batch.getCreatedAt());
            batchMap.put("classTimings", batch.getClassTimings());
            batchMap.put("courseTimings", batch.getClassTimings());
            batchMap.put("duration", batch.getDuration());

            int studentCount = enrollmentRepository.countByCourseTitleIgnoreCase(batch.getSelectCourse());
            batchMap.put("studentsCount", studentCount);

            return batchMap;
        }).collect(Collectors.toList());
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

    public Batch getBatchById(Long id) {
        return batchRepository.findById(id).orElse(null);
    }

    public void deleteBatch(Long id) {
        batchRepository.deleteById(id);
    }
}
