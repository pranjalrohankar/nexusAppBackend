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

    @jakarta.validation.constraints.Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Mobile number must be a valid 10-digit number starting with 6, 7, 8, or 9"
    )
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

    @jakarta.validation.constraints.Pattern(
        regexp = "^$|^[6-9]\\d{9}$",
        message = "Guardian mobile number must be a valid 10-digit number starting with 6, 7, 8, or 9"
    )
    @JsonProperty("guardianPhone")
    private String guardianPhone;

    private String course;

    @JsonProperty("batchName")
    private String batchName;

    @JsonProperty("batchId")
    private Long batchId;

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

    @JsonProperty("courseIds")
    private java.util.List<Integer> courseIds;

    private String password;

    @NotBlank
    private String role;
}
