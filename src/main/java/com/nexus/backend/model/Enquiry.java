package com.nexus.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity representing a student enquiry submitted via the Enquiry Form.
 * Stored in the "enquiries" table in the database.
 * When a new user fills the enquiry form and clicks "Create Account",
 * this record is saved to the database AND a notification email is sent to the admin.
 */
@Entity
@Table(name = "enquiries")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Enquiry {

    // Auto-incremented unique ID for each enquiry record
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Full name of the person submitting the enquiry
    private String fullName;

    // Email address of the enquirer (used by admin to follow up)
    private String email;

    // Phone number of the enquirer
    private String phoneNumber;

    // Optional message or query submitted by the enquirer
    private String message;

    // Course the enquirer is interested in (e.g., "Full Stack Web Development")
    private String course;

    // Whether the enquirer agreed to Terms & Conditions (must be true to submit)
    private boolean termsAccepted;

    // How the enquirer found us: website, whatsapp, referral
    private String source;

    // Timestamp when enquiry was submitted
    private LocalDateTime createdAt;

    // Whether admin has read/viewed this enquiry
    @Column(name = "is_read")
    private Boolean isRead = false;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isRead == null) isRead = false;
    }
}
