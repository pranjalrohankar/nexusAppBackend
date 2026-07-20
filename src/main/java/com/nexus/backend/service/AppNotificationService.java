package com.nexus.backend.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AppNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AppNotificationService.class);

    private final NotificationService notificationService;

    public AppNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // New Enquiry
    public void notifyNewEnquiry(String studentName) {

        logger.info("Creating notification for new enquiry submitted by: {}", studentName);

        notificationService.createNotification(
                "New Enquiry",
                studentName + " submitted a new enquiry.",
                "ADMIN");

        logger.info("New enquiry notification created successfully.");
    }

    // Student Registration
    public void notifyStudentRegistration(String studentName) {

        logger.info("Creating student registration notifications for: {}", studentName);

        notificationService.createNotification(
                "Student Registered",
                studentName + " registered successfully.",
                "ADMIN");

        notificationService.createNotification(
                "New Student",
                studentName + " joined your batch.",
                "TEACHER");

        logger.info("Student registration notifications created successfully.");
    }

    // Batch Created
    public void notifyBatchCreated(String batchName) {

        logger.info("Creating batch notification for batch: {}", batchName);

        notificationService.createNotification(
                "Batch Created",
                "New Batch " + batchName + " is available.",
                "STUDENT");

        logger.info("Batch notification created successfully.");
    }

    // Payment Completed
    public void notifyPayment(String studentName, Double amount) {

        logger.info("Creating payment notification. Student: {}, Amount: {}", studentName, amount);

        notificationService.createNotification(
                "Payment Received",
                studentName + " paid ₹" + amount,
                "ADMIN");

        logger.info("Payment notification created successfully.");
    }

    // Course Assigned
    public void notifyCourseAssigned(String studentName, String courseName) {

        logger.info("Assigning course '{}' to student '{}'", courseName, studentName);

        notificationService.createNotification(
                "Course Assigned",
                "Course " + courseName + " assigned to " + studentName,
                "STUDENT");

        logger.info("Course assignment notification created successfully.");
    }

    public void notifyEnrollment(String studentName, String courseName) {
        notificationService.createNotification(
                "New Student Enrolled",
                studentName + " enrolled in " + courseName + ".",
                "TEACHER");
    }

    public void notifyMilestone(String courseName, int count) {
        notificationService.createNotification(
                "Milestone Achieved",
                courseName + " reached " + count + " enrollments!",
                "TEACHER");
    }

    public void notifyStudentQuery(String studentName, String courseName) {
        notificationService.createNotification(
                "Student Query",
                studentName + " submitted a query about " + courseName + ".",
                "TEACHER");
    }

    public void notifyScheduleUpdated(String batchName, String schedule) {
        notificationService.createNotification(
                "Schedule Updated",
                "Batch " + batchName + " schedule updated to " + schedule + ".",
                "TEACHER");
    }

    public void notifyClassStartingSoon(String courseName, String batchName) {
        notificationService.createNotification(
                "Class Starting Soon",
                courseName + " (" + batchName + ") starts in 15 minutes.",
                "TEACHER");
    }
}