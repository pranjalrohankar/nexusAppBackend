package com.nexus.backend.controller;

import com.nexus.backend.dto.ApiResponse;
import com.nexus.backend.dto.CourseDto;
import com.nexus.backend.model.Course;
import com.nexus.backend.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @PostMapping
    @SuppressWarnings("null")
    public ResponseEntity<CourseDto> createCourse(@Valid @RequestBody CourseDto dto) {
        Course saved = service.createCourse(toEntity(dto));
        CourseDto resp = toDto(saved);
        URI location = URI.create("/api/courses/" + resp.getId());
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
    public ResponseEntity<ApiResponse> active() {
        List<CourseDto> list = service.findByStatus(Course.Status.ACTIVE).stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Active courses fetched", list));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse> all() {
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

    // Simple mappers; consider using MapStruct for larger projects
    private CourseDto toDto(Course c) {
        if (c == null) return null;
        return CourseDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .category(c.getCategory())
                .description(c.getDescription())
                .duration(c.getDuration())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .classTimings(c.getClassTimings())
                .maxCapacity(c.getMaxCapacity())
                .price(c.getPrice())
                .status(c.getStatus())
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
        c.setMaxCapacity(d.getMaxCapacity());
        c.setPrice(d.getPrice());
        c.setStatus(d.getStatus());
        return c;
    }
}