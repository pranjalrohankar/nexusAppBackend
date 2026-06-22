package com.nexus.backend.controller;

import com.nexus.backend.dto.BatchDto;
import com.nexus.backend.model.Batch;
import com.nexus.backend.service.BatchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class BatchController{

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    public Batch createBatch(@RequestBody BatchDto request) {
        return batchService.createBatch(request);
    }

    @GetMapping
    public List<Batch> getAllBatches() {
        return batchService.getAllBatches();
    }

//    @GetMapping("/{id}")
//    public Batch getBatchById(@PathVariable Long id) {
//        return batchService.getBatchById(id);
//    }
//
//    @DeleteMapping("/{id}")
//    public String deleteBatch(@PathVariable Long id) {
//        batchService.deleteBatch(id);
//        return "Batch deleted successfully";
//    }

}