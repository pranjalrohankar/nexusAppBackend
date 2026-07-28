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

    public CourseController(CourseService service, EnrollmentRepository enrollmentRepository) {
        this.service = service;
        this.enrollmentRepository = enrollmentRepository;
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
        return ResponseEntity.ok(toDto(service.getCourseById(id)));
    }

    @GetMapping
    public ResponseEntity<Page<CourseDto>> list(Pageable pageable) {
        Page<Course> page = service.listCourses(pageable);
        Page<CourseDto> dtoPage = page.map(this::toDto);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<CourseDto>> byCategory(@RequestParam String category) {
        List<CourseDto> list = service.findByCategory(category).stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CourseDto>>> active() {
        List<CourseDto> list = service.findByStatus(Course.Status.ACTIVE).stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Active courses fetched", list));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CourseDto>>> all() {
        List<CourseDto> list = service.listAllCourses().stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("All courses fetched", list));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<CourseDto>> byStatus(@RequestParam Course.Status status) {
        List<CourseDto> list = service.findByStatus(status).stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDto> update(@PathVariable Long id, @Valid @RequestBody CourseDto dto) {
        Course updated = service.updateCourse(id, toEntity(dto));
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
            existing.setGoogleMeetLink(link);
            existing.setMeetLink(link);
            service.saveCourse(existing);
            return ResponseEntity.ok(Map.of("success", true, "googleMeetLink", link != null ? link : ""));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Simple mappers; consider using MapStruct for larger projects
    private CourseDto toDto(Course c) {
        if (c == null) return null;
        String meetLink = c.getGoogleMeetLink() != null ? c.getGoogleMeetLink()
                        : c.getMeetLink();
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
                .enrollmentCount(enrollmentRepository.countByCourseTitleIgnoreCase(c.getTitle()))
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
        return c;
    }
}