package com.nexus.backend.dto;

import com.nexus.backend.enums.BatchStatus;
import com.nexus.backend.enums.ClassDay;

import java.time.LocalDate;
import java.util.List;

public class BatchDto{

    private String batchName;
    private String selectCourse;
    private String instructor;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<ClassDay> classDays;
    private BatchStatus status;

    public String getBatchName() {
        return batchName;
    }
    public String getselectCourse() {
        return selectCourse;
    }
    public String getInstructor() {
        return instructor;
    }
    public LocalDate getStartDate() {
        return startDate;
    }
    public LocalDate getEndDate() {
        return endDate;
    }
    public List<ClassDay> getClassDays() {
        return classDays;
    }
    public BatchStatus getStatus() {
        return status;
    }
}