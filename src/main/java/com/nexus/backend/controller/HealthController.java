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
            "version", "2026-09-08-v6-batches-fix",
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
}
