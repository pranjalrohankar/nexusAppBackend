package com.nexus.backend.controller;

import com.nexus.backend.dto.EnquiryRequest;
import com.nexus.backend.model.Enquiry;
import com.nexus.backend.service.EnquiryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController{

    private final EnquiryService enquiryService;

    public EnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @PostMapping
    public Enquiry createEnquiry(@RequestBody EnquiryRequest request) {
        return enquiryService.saveEnquiry(request);
    }
}