package com.nexus.backend.model;

import com.nexus.backend.enums.BatchStatus;
import com.nexus.backend.enums.ClassDay;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "Batches")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String batchName;
    private String selectCourse;
    private String instructor;

    private LocalDate startDate;
    private LocalDate endDate;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<ClassDay> classDays;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();
}
