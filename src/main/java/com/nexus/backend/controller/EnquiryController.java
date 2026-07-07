package com.nexus.backend.controller;

import com.nexus.backend.dto.EnquiryRequest;
import com.nexus.backend.model.Enquiry;
import com.nexus.backend.service.EnquiryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    private static final Logger logger = LoggerFactory.getLogger(EnquiryController.class);

    private final EnquiryService enquiryService;

    public EnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    // POST /api/enquiries - Student submits enquiry form
    @PostMapping
    public Enquiry createEnquiry(@RequestBody EnquiryRequest request) {

        logger.info("Received enquiry request from: {}", request.getEmail());

        Enquiry enquiry = enquiryService.saveEnquiry(request);

        logger.info("Enquiry created successfully with ID: {}", enquiry.getId());

        return enquiry;
    }

    // GET /api/enquiries - Admin fetches all enquiries
    @GetMapping
    public List<Enquiry> getAllEnquiries() {
        return enquiryService.getAllEnquiries();
    }

    // PATCH /api/enquiries/{id}/read - Mark enquiry as read
    @PatchMapping("/{id}/read")
    public ResponseEntity<Enquiry> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryService.markAsRead(id));
    }
}