package com.nexus.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class HealthController {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private javax.sql.DataSource dataSource;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "nexus-backend",
            "version", "2026-09-08-v7-debug-all",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/db-test")
    public ResponseEntity<Map<String, Object>> dbTest() {
        Map<String, Object> result = new HashMap<>();
        if (dataSource == null) {
            result.put("error", "DataSource is null");
            return ResponseEntity.ok(result);
        }
        try (java.sql.Connection conn = dataSource.getConnection()) {
            result.put("connection", "SUCCESS");
            result.put("catalog", conn.getCatalog());
            result.put("schema", conn.getSchema());

            try {
                Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM courses", Integer.class);
                result.put("courses_count", count);
            } catch (Exception e) {
                result.put("courses_error", e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            try {
                Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM batches", Integer.class);
                result.put("batches_count", count);
            } catch (Exception e) {
                result.put("batches_error", e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            try {
                Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM enquiries", Integer.class);
                result.put("enquiries_count", count);
            } catch (Exception e) {
                result.put("enquiries_error", e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            try {
                Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM teachers", Integer.class);
                result.put("teachers_count", count);
            } catch (Exception e) {
                result.put("teachers_error", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } catch (Exception e) {
            result.put("connection_error", e.getClass().getName() + ": " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.nexus.backend.repository.CourseRepository courseRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.nexus.backend.repository.BatchRepository batchRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.nexus.backend.repository.TeacherRepository teacherRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CourseController courseController;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private TeacherController teacherController;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private BatchController batchController;

    @GetMapping("/debug-all")
    public ResponseEntity<Map<String, Object>> debugAll() {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("courseRepo_count", courseRepository != null ? courseRepository.count() : "null");
        } catch (Throwable t) {
            res.put("courseRepo_err", t.getClass().getName() + ": " + t.getMessage());
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            res.put("courseRepo_stack", sw.toString());
        }
        try {
            res.put("batchRepo_count", batchRepository != null ? batchRepository.count() : "null");
        } catch (Throwable t) {
            res.put("batchRepo_err", t.getClass().getName() + ": " + t.getMessage());
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            res.put("batchRepo_stack", sw.toString());
        }
        try {
            res.put("courseCtrl_all", courseController != null ? courseController.all() : "null");
        } catch (Throwable t) {
            res.put("courseCtrl_err", t.getClass().getName() + ": " + t.getMessage());
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            res.put("courseCtrl_stack", sw.toString());
        }
        try {
            res.put("teacherCtrl_all", teacherController != null ? teacherController.getAllTeachers() : "null");
        } catch (Throwable t) {
            res.put("teacherCtrl_err", t.getClass().getName() + ": " + t.getMessage());
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            res.put("teacherCtrl_stack", sw.toString());
        }
        try {
            res.put("batchCtrl_all", batchController != null ? batchController.getAllBatches() : "null");
        } catch (Throwable t) {
            res.put("batchCtrl_err", t.getClass().getName() + ": " + t.getMessage());
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            res.put("batchCtrl_stack", sw.toString());
        }
        return ResponseEntity.ok(res);
    }
}
