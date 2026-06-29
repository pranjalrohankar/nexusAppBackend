package com.nexus.backend.controller;

import com.nexus.backend.dto.EnquiryRequest;
import com.nexus.backend.model.Enquiry;
import com.nexus.backend.service.EnquiryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController{

    private final EnquiryService enquiryService;

    public EnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    // POST /api/enquiries - Student submits enquiry form (saves to DB + sends email to admin)
    @PostMapping
    public Enquiry createEnquiry(@RequestBody EnquiryRequest request) {
        return enquiryService.saveEnquiry(request);
    }

    // GET /api/enquiries - Admin fetches all enquiries to display in admin panel
    @GetMapping
    public List<Enquiry> getAllEnquiries() {
        return enquiryService.getAllEnquiries();
    }
}