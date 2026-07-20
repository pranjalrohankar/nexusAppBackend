package com.nexus.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "dob")
    private String dob;

    @Column(name = "street")
    private String street;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "pin_code")
    private String pinCode;

    @Column(name = "qualification")
    private String qualification;

    @Column(name = "experience")
    private String experience;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "join_date")
    private String joinDate;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "profile_image")
    private String profileImage;
}
