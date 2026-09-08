package com.nexus.backend.controller;

import com.nexus.backend.dto.ApiResponse;
import com.nexus.backend.dto.TestAttemptRequest;
import com.nexus.backend.dto.TestDto;
import com.nexus.backend.model.TestAttempt;
import com.nexus.backend.model.User;
import com.nexus.backend.repository.UserRepository;
import com.nexus.backend.service.TestAttemptService;
import com.nexus.backend.service.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TestController {

    private final TestService testService;
    private final TestAttemptService testAttemptService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        String email = auth.getName();
        if (email == null) return null;
        return userRepository.findByEmailIgnoreCase(email.trim())
                .or(() -> userRepository.findByEmail(email.trim()))
                .orElse(null);
    }

    /**
     * POST /api/tests - Create and publish a new test/assessment
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createTest(@RequestBody TestDto testDto) {
        try {
            User user = getCurrentUser();
            String creatorEmail = user != null ? user.getEmail() : testDto.getCreatedByTeacherEmail();
            String creatorName = user != null ? user.getName() : testDto.getCreatedByName();

            TestDto created = testService.createTest(testDto, creatorEmail, creatorName);
            return ResponseEntity.ok(ApiResponse.ok("Test created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /api/tests - List tests with optional filter
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getTests(
            @RequestParam(required = false) String courseTitle,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String teacherEmail) {
        if (courseTitle != null && !courseTitle.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok("Tests fetched", testService.getTestsByCourse(courseTitle)));
        }
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok("Tests fetched", testService.getTestsByCategory(category)));
        }
        if (teacherEmail != null && !teacherEmail.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok("Tests fetched", testService.getTestsByTeacher(teacherEmail)));
        }
        return ResponseEntity.ok(ApiResponse.ok("All tests fetched", testService.getAllTests()));
    }

    /**
     * GET /api/tests/all - List all tests
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getAllTests() {
        return ResponseEntity.ok(ApiResponse.ok("All tests fetched", testService.getAllTests()));
    }

    /**
     * GET /api/tests/{id} - Get test by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getTestById(@PathVariable Long id) {
        var opt = testService.getTestById(id);
        if (opt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.ok("Test fetched", opt.get()));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Test not found"));
    }

    /**
     * GET /api/tests/course/{courseTitle} - Get tests by course
     */
    @GetMapping("/course/{courseTitle}")
    public ResponseEntity<ApiResponse<?>> getTestsByCourse(@PathVariable String courseTitle) {
        List<TestDto> list = testService.getTestsByCourse(courseTitle);
        return ResponseEntity.ok(ApiResponse.ok("Tests for course fetched", list));
    }

    /**
     * DELETE /api/tests/{id} - Delete test
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteTest(@PathVariable Long id) {
        boolean deleted = testService.deleteTest(id);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.ok("Test deleted successfully", null));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Test not found"));
    }

    private Map<String, Object> toSubmissionDto(TestAttempt sub) {
        if (sub == null) return new HashMap<>();
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", sub.getId());
        dto.put("testId", sub.getTestId());
        dto.put("studentId", sub.getStudentId());
        dto.put("testTitle", sub.getTestTitle() != null ? sub.getTestTitle() : "Assessment");
        dto.put("studentName", sub.getStudentName() != null ? sub.getStudentName() : "Student");
        dto.put("studentEmail", sub.getStudentEmail() != null ? sub.getStudentEmail() : "");
        dto.put("marksObtained", sub.getMarksObtained());
        dto.put("obtainedMarks", sub.getMarksObtained());
        dto.put("totalMarks", sub.getTotalMarks() != null ? sub.getTotalMarks() : 100);
        dto.put("status", sub.getStatus() != null ? sub.getStatus() : "PENDING");
        dto.put("answersJson", sub.getAnswersJson());
        dto.put("answersText", sub.getAnswersText());
        dto.put("solutionFileName", sub.getSolutionFileName());
        dto.put("solutionFileUri", sub.getSolutionFileUri());
        dto.put("feedback", sub.getFeedback());
        dto.put("attemptDate", sub.getAttemptDate());
        dto.put("attemptTime", sub.getAttemptTime());
        dto.put("submittedAt", sub.getSubmittedAt() != null ? sub.getSubmittedAt().toString() : "");
        return dto;
    }

    /**
     * POST /api/tests/submit - Submit test attempt / assessment answers
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<?>> submitTestAttempt(@RequestBody TestAttemptRequest request) {
        try {
            User user = getCurrentUser();
            String email = user != null ? user.getEmail() : request.getStudentEmail();
            String name = user != null ? user.getName() : request.getStudentName();

            TestAttempt attempt = testAttemptService.submitAttempt(request, email, name);
            return ResponseEntity.ok(ApiResponse.ok("Test submitted successfully", toSubmissionDto(attempt)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /api/tests/submissions - Get submissions for grading/review
     */
    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<?>> getSubmissions(@RequestParam(required = false) String status) {
        List<TestAttempt> submissions = testAttemptService.getSubmissions(status);
        List<Map<String, Object>> dtoList = submissions.stream()
                .map(this::toSubmissionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Submissions fetched", dtoList));
    }

    /**
     * GET /api/tests/submissions/my - Get submissions for the logged in student
     */
    @GetMapping("/submissions/my")
    public ResponseEntity<ApiResponse<?>> getMySubmissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = getCurrentUser();
        String email = user != null ? user.getEmail() : (auth != null ? auth.getName() : null);
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        List<TestAttempt> list = testAttemptService.getAttemptsByStudentEmail(email);
        List<Map<String, Object>> dtoList = list.stream()
                .map(this::toSubmissionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("My submissions fetched", dtoList));
    }

    /**
     * PUT /api/tests/submissions/{id}/grade - Grade a submission
     */
    @PutMapping("/submissions/{id}/grade")
    public ResponseEntity<ApiResponse<?>> gradeSubmission(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Integer marks = body.get("marks") != null ? Integer.valueOf(body.get("marks").toString()) : null;
        String feedback = body.get("feedback") != null ? body.get("feedback").toString() : null;
        String status = body.get("status") != null ? body.get("status").toString() : "GRADED";

        var opt = testAttemptService.gradeSubmission(id, marks, feedback, status);
        if (opt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.ok("Submission graded successfully", toSubmissionDto(opt.get())));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Submission not found"));
    }
}
