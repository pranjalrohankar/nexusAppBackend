package com.nexus.backend.controller;

import com.nexus.backend.dto.BatchDto;
import com.nexus.backend.model.Batch;
import com.nexus.backend.service.BatchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Handles HTTP requests for batch management
@RestController
@RequestMapping("/api/batches")
public class BatchController{

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    // POST /api/batches - Admin creates a new batch
    @PostMapping
    public Batch createBatch(@RequestBody BatchDto request) {
        return batchService.createBatch(request);
    }

    // GET /api/batches - Returns all batches from the database
    @GetMapping
    public List<Batch> getAllBatches() {
        return batchService.getAllBatches();
    }


}