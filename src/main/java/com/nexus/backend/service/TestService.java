package com.nexus.backend.service;

import com.nexus.backend.dto.TestDto;
import com.nexus.backend.model.Test;
import com.nexus.backend.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;

    @Transactional
    public TestDto createTest(TestDto dto, String creatorEmail, String creatorName) {
        String testTitle = dto.getTitle() != null && !dto.getTitle().isBlank() 
                ? dto.getTitle() : dto.getTestName();
        if (testTitle == null || testTitle.isBlank()) {
            throw new IllegalArgumentException("Test title is required");
        }

        Test test = Test.builder()
                .testName(testTitle)
                .courseTitle(dto.getCourseTitle())
                .category(dto.getCategory())
                .duration(dto.getDuration())
                .passScore(dto.getPassScore())
                .totalMarks(dto.getTotalMarks() != null ? dto.getTotalMarks() : 100)
                .testType(dto.getTestType() != null ? dto.getTestType() : "MCQ")
                .questionsCount(dto.getQuestionsCount())
                .pdfFileName(dto.getPdfFileName())
                .pdfFileUri(dto.getPdfFileUri())
                .pdfInstructions(dto.getPdfInstructions())
                .questionsJson(dto.getQuestionsJson())
                .createdByTeacherEmail(creatorEmail != null ? creatorEmail : dto.getCreatedByTeacherEmail())
                .createdByName(creatorName != null ? creatorName : dto.getCreatedByName())
                .testDate(dto.getTestDate())
                .testTime(dto.getTestTime())
                .createdAt(LocalDateTime.now())
                .build();

        Test saved = testRepository.save(test);
        return TestDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TestDto> getAllTests() {
        return testRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(TestDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<TestDto> getTestById(Long id) {
        return testRepository.findById(id).map(TestDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<TestDto> getTestsByCourse(String courseTitle) {
        if (courseTitle == null || courseTitle.isBlank()) {
            return getAllTests();
        }
        return testRepository.findByCourseTitleIgnoreCase(courseTitle.trim()).stream()
                .map(TestDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestDto> getTestsByCategory(String category) {
        if (category == null || category.isBlank()) {
            return getAllTests();
        }
        return testRepository.findByCategoryIgnoreCase(category.trim()).stream()
                .map(TestDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestDto> getTestsByTeacher(String teacherEmail) {
        if (teacherEmail == null || teacherEmail.isBlank()) {
            return getAllTests();
        }
        return testRepository.findByCreatedByTeacherEmailIgnoreCase(teacherEmail.trim()).stream()
                .map(TestDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean deleteTest(Long id) {
        if (testRepository.existsById(id)) {
            testRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
