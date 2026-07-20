package com.nexus.backend.service;

import com.nexus.backend.dto.EnquiryRequest;
import com.nexus.backend.exception.BadRequestException;
import com.nexus.backend.model.Enquiry;
import com.nexus.backend.repository.BatchRepository;
import com.nexus.backend.repository.EnquiryRepository;
import com.nexus.backend.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Sort;

@Service
public class EnquiryService {

    private static final Logger logger = LoggerFactory.getLogger(EnquiryService.class);

    private final EnquiryRepository enquiryRepository;
    private final EmailService emailService;
    private final AppNotificationService appNotificationService;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    @Value("${nexus.enquiry.admin-email:adityanale1831@gmail.com}")
    private String adminEmail;

    public EnquiryService(EnquiryRepository enquiryRepository, EmailService emailService,
                          AppNotificationService appNotificationService,
                          BatchRepository batchRepository, UserRepository userRepository) {
        this.enquiryRepository = enquiryRepository;
        this.emailService = emailService;
        this.appNotificationService = appNotificationService;
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
    }

    // Mark enquiry as read
    public Enquiry markAsRead(Long id) {
        Enquiry enquiry = enquiryRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Enquiry not found: " + id));
        enquiry.setIsRead(true);
        return enquiryRepository.save(enquiry);
    }

    // Fetch all enquiries
    public List<Enquiry> getAllEnquiries() {
        return enquiryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // Save enquiry
    public Enquiry saveEnquiry(EnquiryRequest request) {

        logger.info("Received enquiry request from email: {}", request.getEmail());

        if (request.getFullName() == null || request.getFullName().isBlank()) {
            logger.warn("Validation failed: Full name is missing.");
            throw new BadRequestException("Full name is required");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            logger.warn("Validation failed: Email is missing.");
            throw new BadRequestException("Email is required");
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            logger.warn("Validation failed: Phone number is missing.");
            throw new BadRequestException("Phone number is required");
        }

        if (!request.isTermsAccepted()) {
            logger.warn("Validation failed: Terms and conditions not accepted.");
            throw new BadRequestException("Please accept terms and conditions");
        }

        Enquiry enquiry = new Enquiry();
        enquiry.setFullName(request.getFullName());
        enquiry.setEmail(request.getEmail());
        enquiry.setPhoneNumber(request.getPhoneNumber());
        enquiry.setMessage(request.getMessage());
        enquiry.setCourse(request.getCourse());
        enquiry.setTermsAccepted(request.isTermsAccepted());
        enquiry.setSource(request.getSource());

        logger.info("Saving enquiry for student: {}", enquiry.getFullName());

        Enquiry saved = enquiryRepository.save(enquiry);

        logger.info("Enquiry saved successfully with ID: {}", saved.getId());

        logger.info("Sending enquiry notification email to admin: {}", adminEmail);

        emailService.sendEnquiryNotification(adminEmail, saved);

        logger.info("Admin notification email sent successfully.");

        appNotificationService.notifyNewEnquiry(saved.getFullName());

        // Send to specific teacher assigned to this course
        if (saved.getCourse() != null && !saved.getCourse().isBlank()) {
            batchRepository.findAll().stream()
                .filter(b -> b.getSelectCourse() != null
                    && b.getSelectCourse().equalsIgnoreCase(saved.getCourse())
                    && b.getInstructor() != null && !b.getInstructor().isBlank())
                .findFirst()
                .ifPresent(b -> userRepository.findAll().stream()
                    .filter(u -> u.getName() != null && u.getName().equalsIgnoreCase(b.getInstructor()))
                    .map(u -> u.getEmail()).findFirst()
                    .ifPresent(email -> appNotificationService.notifyStudentQuery(
                        saved.getFullName(), saved.getCourse(), email)));
        }

        return saved;
    }
}