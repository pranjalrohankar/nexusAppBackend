package com.nexus.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
