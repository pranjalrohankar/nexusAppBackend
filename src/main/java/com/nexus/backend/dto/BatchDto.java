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
    private String classTimings;
    private String duration;
    private String googleMeetLink;
    private String coveredTopics;

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public String getSelectCourse() { return selectCourse; }
    public void setSelectCourse(String selectCourse) { this.selectCourse = selectCourse; }

    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<ClassDay> getClassDays() { return classDays; }
    public void setClassDays(List<ClassDay> classDays) { this.classDays = classDays; }

    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }

    public String getClassTimings() { return classTimings; }
    public void setClassTimings(String classTimings) { this.classTimings = classTimings; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getGoogleMeetLink() { return googleMeetLink; }
    public void setGoogleMeetLink(String googleMeetLink) { this.googleMeetLink = googleMeetLink; }

    public String getCoveredTopics() { return coveredTopics; }
    public void setCoveredTopics(String coveredTopics) { this.coveredTopics = coveredTopics; }
}