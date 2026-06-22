package com.nexus.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "StudentMaterial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data

public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


}