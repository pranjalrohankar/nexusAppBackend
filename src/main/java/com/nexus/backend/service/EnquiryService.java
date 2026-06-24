package com.nexus.backend.service;

import com.nexus.backend.dto.EnquiryRequest;
import com.nexus.backend.model.Enquiry;
import com.nexus.backend.repository.EnquiryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final EmailService emailService;

    @Value("${nexus.enquiry.admin-email:adityanale1831@gmail.com}")
    private String adminEmail;

    public EnquiryService(EnquiryRepository enquiryRepository, EmailService emailService) {
        this.enquiryRepository = enquiryRepository;
        this.emailService = emailService;
    }

    public Enquiry saveEnquiry(EnquiryRequest request) {
        Enquiry enquiry = new Enquiry();
        enquiry.setFullName(request.getFullName());
        enquiry.setEmail(request.getEmail());
        enquiry.setPhoneNumber(request.getPhoneNumber());
        enquiry.setMessage(request.getMessage());
        enquiry.setCourse(request.getCourse());
        enquiry.setTermsAccepted(request.isTermsAccepted());

        Enquiry saved = enquiryRepository.save(enquiry);
        emailService.sendEnquiryNotification(adminEmail, saved);
        return saved;
    }
}