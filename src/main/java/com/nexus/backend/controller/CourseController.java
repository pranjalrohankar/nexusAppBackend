package com.nexus.backend.controller;

import com.nexus.backend.dto.ApiResponse;
import com.nexus.backend.dto.CourseDto;
import com.nexus.backend.model.Course;
import com.nexus.backend.repository.EnrollmentRepository;
import com.nexus.backend.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService service;
    private final EnrollmentRepository enrollmentRepository;
    private final com.nexus.backend.repository.BatchRepository batchRepository;

    public CourseController(CourseService service, EnrollmentRepository enrollmentRepository,
                            com.nexus.backend.repository.BatchRepository batchRepository) {
        this.service = service;
        this.enrollmentRepository = enrollmentRepository;
        this.batchRepository = batchRepository;
    }

    @PostMapping
    @SuppressWarnings("null")
    public ResponseEntity<CourseDto> createCourse(@Valid @RequestBody CourseDto dto) {
        Course saved = service.createCourse(toEntity(dto));
        CourseDto resp = toDto(saved);
        URI location = UriComponentsBuilder
                .fromPath("/api/courses/{id}")
                .buildAndExpand(resp.getId())
                .toUri();
        return ResponseEntity.created(location).body(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDto> getById(@PathVariable Long id) {
        try {
            Course c = service.getCourseById(id);
            return ResponseEntity.ok(toDto(c));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<CourseDto>> list(Pageable pageable) {
        try {
            Page<Course> page = service.listCourses(pageable);
            Page<CourseDto> dtoPage = page.map(this::toDto);
            return ResponseEntity.ok(dtoPage);
        } catch (Exception e) {
            return ResponseEntity.ok(Page.empty());
        }
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<CourseDto>> byCategory(@RequestParam String category) {
        try {
            List<CourseDto> list = service.findByCategory(category).stream().map(this::toDto).collect(Collectors.toList());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CourseDto>>> active() {
        try {
            List<CourseDto> list = service.findByStatus(Course.Status.ACTIVE).stream().map(this::toDto).collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.ok("Active courses fetched", list));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.ok("Active courses fallback", java.util.Collections.emptyList()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CourseDto>>> all() {
        try {
            List<CourseDto> list = service.listAllCourses().stream().map(this::toDto).collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.ok("All courses fetched", list));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.ok("Courses fallback", java.util.Collections.emptyList()));
        }
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<CourseDto>> byStatus(@RequestParam Course.Status status) {
        try {
            List<CourseDto> list = service.findByStatus(status).stream().map(this::toDto).collect(Collectors.toList());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDto> update(@PathVariable Long id, @Valid @RequestBody CourseDto dto) {
        Course updated = service.updateCourse(id, toEntity(dto));
        if (dto.getGoogleMeetLink() != null) {
            batchRepository.findAll().stream()
                .filter(b -> b.getSelectCourse() != null && b.getSelectCourse().equalsIgnoreCase(updated.getTitle()))
                .forEach(b -> {
                    b.setGoogleMeetLink(dto.getGoogleMeetLink());
                    batchRepository.save(b);
                });
        }
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/meet-link")
    public ResponseEntity<Map<String, Object>> updateMeetLink(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            Course existing = service.getCourseById(id);
            String link = body.get("googleMeetLink");
            if (link == null) link = body.get("meetLink");
            existing.setGoogleMeetLink(link);
            existing.setMeetLink(link);
            service.saveCourse(existing);

            final String finalLink = link;
            if (existing.getTitle() != null) {
                batchRepository.findAll().stream()
                    .filter(b -> b.getSelectCourse() != null && b.getSelectCourse().equalsIgnoreCase(existing.getTitle()))
                    .forEach(b -> {
                        b.setGoogleMeetLink(finalLink);
                        batchRepository.save(b);
                    });
            }
            return ResponseEntity.ok(Map.of("success", true, "googleMeetLink", link != null ? link : ""));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/covered-topics")
    public ResponseEntity<Map<String, Object>> updateCourseCoveredTopics(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Course course = service.getCourseById(id);
            Object raw = body.get("coveredTopics");
            if (raw == null) raw = body.get("topics");
            String topicsStr = "";
            if (raw instanceof List) {
                topicsStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(raw);
            } else if (raw != null) {
                topicsStr = raw.toString();
            }
            if (course != null) {
                course.setCoveredTopics(topicsStr);
                service.saveCourse(course);

                // Sync to matching batches
                if (course.getTitle() != null) {
                    final String finalTopics = topicsStr;
                    final String cleanTitle = course.getTitle().trim().toLowerCase();
                    batchRepository.findAll().stream()
                        .filter(b -> b.getSelectCourse() != null && (
                            b.getSelectCourse().trim().equalsIgnoreCase(course.getTitle().trim()) ||
                            cleanTitle.contains(b.getSelectCourse().trim().toLowerCase()) ||
                            b.getSelectCourse().trim().toLowerCase().contains(cleanTitle)
                        ))
                        .forEach(b -> {
                            b.setCoveredTopics(finalTopics);
                            batchRepository.save(b);
                        });
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "courseId", id, "coveredTopics", topicsStr));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", true, "courseId", id, "warning", e.getMessage()));
        }
    }

    @GetMapping("/{id}/covered-topics")
    public ResponseEntity<Map<String, Object>> getCourseCoveredTopics(@PathVariable Long id) {
        try {
            Course course = service.getCourseById(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "courseId", id,
                "coveredTopics", course != null && course.getCoveredTopics() != null ? course.getCoveredTopics() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", true, "courseId", id, "coveredTopics", ""));
        }
    }

    @PutMapping("/by-title/covered-topics")
    public ResponseEntity<Map<String, Object>> updateCoveredTopicsByTitle(
            @RequestParam String title,
            @RequestBody Map<String, Object> body) {
        try {
            Object raw = body.get("coveredTopics");
            if (raw == null) raw = body.get("topics");
            String topicsStr = "";
            if (raw instanceof List) {
                topicsStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(raw);
            } else if (raw != null) {
                topicsStr = raw.toString();
            }
            final String finalTopics = topicsStr;
            final String search = title != null ? title.trim().toLowerCase() : "";

            if (!search.isEmpty()) {
                service.listAllCourses().stream()
                    .filter(c -> c.getTitle() != null && (
                        c.getTitle().trim().equalsIgnoreCase(title.trim()) ||
                        c.getTitle().trim().toLowerCase().contains(search) ||
                        search.contains(c.getTitle().trim().toLowerCase())
                    ))
                    .forEach(c -> {
                        c.setCoveredTopics(finalTopics);
                        service.saveCourse(c);
                    });

                batchRepository.findAll().stream()
                    .filter(b -> b.getSelectCourse() != null && (
                        b.getSelectCourse().trim().equalsIgnoreCase(title.trim()) ||
                        b.getSelectCourse().trim().toLowerCase().contains(search) ||
                        search.contains(b.getSelectCourse().trim().toLowerCase())
                    ))
                    .forEach(b -> {
                        b.setCoveredTopics(finalTopics);
                        batchRepository.save(b);
                    });
            }

            return ResponseEntity.ok(Map.of("success", true, "title", title != null ? title : "", "coveredTopics", topicsStr));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", true, "title", title != null ? title : "", "warning", e.getMessage()));
        }
    }

    @GetMapping("/by-title/covered-topics")
    public ResponseEntity<Map<String, Object>> getCoveredTopicsByTitle(@RequestParam String title) {
        String topics = service.listAllCourses().stream()
            .filter(c -> c.getTitle() != null && c.getTitle().equalsIgnoreCase(title.trim()))
            .map(Course::getCoveredTopics)
            .filter(t -> t != null && !t.isBlank())
            .findFirst().orElse("");
        if (topics.isBlank()) {
            topics = batchRepository.findAll().stream()
                .filter(b -> b.getSelectCourse() != null && b.getSelectCourse().equalsIgnoreCase(title.trim()))
                .map(com.nexus.backend.model.Batch::getCoveredTopics)
                .filter(t -> t != null && !t.isBlank())
                .findFirst().orElse("");
        }
        return ResponseEntity.ok(Map.of("success", true, "title", title, "coveredTopics", topics));
    }

    // Simple mappers; consider using MapStruct for larger projects
    private CourseDto toDto(Course c) {
        if (c == null) return null;
        String meetLink = c.getGoogleMeetLink() != null ? c.getGoogleMeetLink()
                        : c.getMeetLink();
        int enrollCount = 0;
        if (c.getTitle() != null && !c.getTitle().isBlank()) {
            try {
                enrollCount = enrollmentRepository.countByCourseTitleIgnoreCase(c.getTitle().trim());
            } catch (Exception ignored) {}
        }
        return CourseDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .category(c.getCategory())
                .description(c.getDescription())
                .duration(c.getDuration())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .classTimings(c.getClassTimings())
                .classDays(c.getClassDays())
                .instructor(c.getInstructor())
                .syllabusTopics(c.getSyllabusTopics())
                .whatYouWillLearn(c.getWhatYouWillLearn())
                .googleMeetLink(meetLink)
                .totalSessions(c.getTotalSessions())
                .maxCapacity(c.getMaxCapacity())
                .price(c.getPrice())
                .status(c.getStatus())
                .enrollmentCount(enrollCount)
                .coveredTopics(c.getCoveredTopics() != null ? c.getCoveredTopics() : "")
                .build();
    }

    private Course toEntity(CourseDto d) {
        if (d == null) return null;
        Course c = new Course();
        c.setId(d.getId());
        c.setTitle(d.getTitle());
        c.setCategory(d.getCategory());
        c.setDescription(d.getDescription());
        c.setDuration(d.getDuration());
        c.setStartDate(d.getStartDate());
        c.setEndDate(d.getEndDate());
        c.setClassTimings(d.getClassTimings());
        c.setClassDays(d.getClassDays());
        c.setInstructor(d.getInstructor());
        c.setSyllabusTopics(d.getSyllabusTopics());
        c.setWhatYouWillLearn(d.getWhatYouWillLearn());
        c.setGoogleMeetLink(d.getGoogleMeetLink());
        c.setMeetLink(d.getGoogleMeetLink());
        c.setTotalSessions(d.getTotalSessions());
        c.setMaxCapacity(d.getMaxCapacity());
        c.setPrice(d.getPrice());
        c.setStatus(d.getStatus());
        if (d.getCoveredTopics() != null) {
            c.setCoveredTopics(d.getCoveredTopics());
        }
        return c;
    }
}