package com.nexus.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String firstName;
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String phone;

    private String role; // "STUDENT", "TEACHER", "ADMIN"

    // Optional profile fields
    private String dob;
    private String street;
    private String city;
    private String state;

    @JsonProperty("pinCode")
    private String pinCode;

    // Student fields
    @JsonProperty("guardianName")
    private String guardianName;

    @JsonProperty("guardianPhone")
    private String guardianPhone;

    private String course;

    // Teacher fields
    private String qualification;
    private String experience;
    private String specialization;

    @JsonProperty("courseIds")
    private List<Integer> courseIds;
}
