package com.nexus.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    private String phone;

    private String dob;
    private String street;
    private String city;
    private String state;

    @JsonProperty("pinCode")
    private String pinCode;

    // Student specific
    @JsonProperty("guardianName")
    private String guardianName;

    @JsonProperty("guardianPhone")
    private String guardianPhone;

    private String course;

    @JsonProperty("enrollmentDate")
    private String enrollmentDate;

    @JsonProperty("paymentStatus")
    private String paymentStatus;

    // Teacher specific
    private String qualification;
    private String experience;
    private String specialization;

    @JsonProperty("joinDate")
    private String joinDate;

    @JsonProperty("employmentType")
    private String employmentType;

    private String password;

    @NotBlank
    private String role;
}
