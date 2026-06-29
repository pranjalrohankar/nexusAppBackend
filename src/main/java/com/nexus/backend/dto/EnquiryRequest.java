package com.nexus.backend.dto;

public class EnquiryRequest {

    private String fullName;
    private String email;
    private String phoneNumber;
    private String message;
    private String course;
    private boolean termsAccepted;

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public String getCourse() {
        return course;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }
}