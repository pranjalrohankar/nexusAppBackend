package com.nexus.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tests")
@Data
@NoArgsConstructor
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "course_title")
    private String courseTitle;

    @Column(name = "total_marks")
    private Integer totalMarks;

    @Column(name = "test_date")
    private String testDate;

    @Column(name = "test_time")
    private String testTime;
}
