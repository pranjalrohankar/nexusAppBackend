package com.nexus.backend.service;

import com.nexus.backend.dto.TestAttemptRequest;
import com.nexus.backend.model.Student;
import com.nexus.backend.model.Test;
import com.nexus.backend.model.TestAttempt;
import com.nexus.backend.repository.StudentRepository;
import com.nexus.backend.repository.TestAttemptRepository;
import com.nexus.backend.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TestAttemptService {

    private final TestAttemptRepository testAttemptRepository;
    private final TestRepository testRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public TestAttempt submitAttempt(TestAttemptRequest req, String userEmail, String userName) {
        Student student = null;
        if (req.getStudentId() != null) {
            student = studentRepository.findById(req.getStudentId()).orElse(null);
        }
        if (student == null && userEmail != null) {
            student = studentRepository.findByEmail(userEmail).orElse(null);
        }

        Test test = null;
        if (req.getTestId() != null) {
            test = testRepository.findById(req.getTestId()).orElse(null);
        }

        String effectiveStudentName = req.getStudentName();
        if (effectiveStudentName == null || effectiveStudentName.isBlank()) {
            if (student != null && student.getName() != null) {
                effectiveStudentName = student.getName();
            } else if (userName != null) {
                effectiveStudentName = userName;
            } else {
                effectiveStudentName = "Student";
            }
        }

        String effectiveStudentEmail = req.getStudentEmail();
        if (effectiveStudentEmail == null || effectiveStudentEmail.isBlank()) {
            if (student != null && student.getEmail() != null) {
                effectiveStudentEmail = student.getEmail();
            } else if (userEmail != null) {
                effectiveStudentEmail = userEmail;
            }
        }

        String testTitle = req.getTestTitle();
        if (testTitle == null || testTitle.isBlank()) {
            if (test != null) {
                testTitle = test.getTestName();
            } else {
                testTitle = "Assessment";
            }
        }

        Integer totalMarks = req.getTotalMarks();
        if (totalMarks == null && test != null) {
            totalMarks = test.getTotalMarks();
        }
        if (totalMarks == null) {
            totalMarks = 100;
        }

        String status = req.getStatus();
        if (status == null || status.isBlank()) {
            status = req.getMarksObtained() != null ? "GRADED" : "PENDING";
        }

        String todayDate = req.getAttemptDate() != null ? req.getAttemptDate() : LocalDate.now().toString();
        String nowTime = req.getAttemptTime() != null ? req.getAttemptTime() : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        TestAttempt attempt = TestAttempt.builder()
                .student(student)
                .test(test)
                .testTitle(testTitle)
                .studentName(effectiveStudentName)
                .studentEmail(effectiveStudentEmail)
                .marksObtained(req.getMarksObtained())
                .totalMarks(totalMarks)
                .status(status)
                .answersJson(req.getAnswersJson())
                .answersText(req.getAnswersText())
                .solutionFileName(req.getSolutionFileName())
                .solutionFileUri(req.getSolutionFileUri())
                .feedback(req.getFeedback())
                .attemptDate(todayDate)
                .attemptTime(nowTime)
                .submittedAt(LocalDateTime.now())
                .build();

        return testAttemptRepository.save(attempt);
    }

    @Transactional
    public Optional<TestAttempt> gradeSubmission(Long attemptId, Integer marks, String feedback, String status) {
        return testAttemptRepository.findById(attemptId).map(attempt -> {
            if (marks != null) attempt.setMarksObtained(marks);
            if (feedback != null) attempt.setFeedback(feedback);
            attempt.setStatus(status != null && !status.isBlank() ? status : "GRADED");
            return testAttemptRepository.save(attempt);
        });
    }

    @Transactional(readOnly = true)
    public List<TestAttempt> getSubmissions(String status) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return testAttemptRepository.findByStatus(status.toUpperCase());
        }
        return testAttemptRepository.findAllByOrderBySubmittedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<TestAttempt> getAttemptsByStudent(Long studentId) {
        return testAttemptRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<TestAttempt> getAttemptsByStudentEmail(String studentEmail) {
        return testAttemptRepository.findByStudentEmailIgnoreCaseOrderBySubmittedAtDesc(studentEmail);
    }

    @Transactional(readOnly = true)
    public List<TestAttempt> getAttemptsByTest(Long testId) {
        return testAttemptRepository.findByTestId(testId);
    }
}
