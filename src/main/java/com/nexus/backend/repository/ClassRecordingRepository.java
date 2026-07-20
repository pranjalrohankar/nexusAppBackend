package com.nexus.backend.repository;

import com.nexus.backend.model.ClassRecording;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassRecordingRepository  extends JpaRepository<ClassRecording, Long>{
    List<ClassRecording> findByCourseIgnoreCase(String course);
}