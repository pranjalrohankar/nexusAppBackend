package com.nexus.backend.service;

import com.nexus.backend.dto.BatchDto;
import com.nexus.backend.model.Batch;
import com.nexus.backend.repository.BatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BatchService {

    @Autowired
    private BatchRepository batchRepository;

    public BatchService(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public Batch createBatch(BatchDto request) {

        Batch batch = new Batch();

        batch.setBatchName(request.getBatchName());

        batch.setSelectCourse(request.getselectCourse());
        batch.setInstructor(request.getInstructor());

        batch.setStartDate(request.getStartDate());
        batch.setEndDate(request.getEndDate());

        batch.setStatus(request.getStatus());
        batch.setClassDays(request.getClassDays());
        return batchRepository.save(batch);
    }

    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

}