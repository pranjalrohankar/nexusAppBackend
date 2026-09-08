package com.nexus.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaRepairRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Starting automated database schema repairs and migrations...");

        List<String> ddlStatements = List.of(
            // Enquiries
            "ALTER TABLE IF EXISTS enquiries ADD COLUMN IF NOT EXISTS source VARCHAR(100)",
            "ALTER TABLE IF EXISTS enquiries ADD COLUMN IF NOT EXISTS created_at TIMESTAMP",
            "ALTER TABLE IF EXISTS enquiries ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT false",
            "UPDATE enquiries SET is_read = false WHERE is_read IS NULL",

            // Security Settings
            "ALTER TABLE IF EXISTS security_settings ADD COLUMN IF NOT EXISTS activity_status_enabled BOOLEAN DEFAULT true",
            "ALTER TABLE IF EXISTS security_settings ADD COLUMN IF NOT EXISTS online BOOLEAN DEFAULT false",
            "UPDATE security_settings SET activity_status_enabled = true WHERE activity_status_enabled IS NULL",
            "UPDATE security_settings SET online = false WHERE online IS NULL",

            // Courses table variations
            "ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS covered_topics TEXT",
            "ALTER TABLE IF EXISTS \"courses\" ADD COLUMN IF NOT EXISTS covered_topics TEXT",
            "ALTER TABLE IF EXISTS \"Courses\" ADD COLUMN IF NOT EXISTS covered_topics TEXT",
            "ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"courses\" ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"Courses\" ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255)",
            "ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS meet_link VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"courses\" ADD COLUMN IF NOT EXISTS meet_link VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"Courses\" ADD COLUMN IF NOT EXISTS meet_link VARCHAR(255)",
            "ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS total_sessions INTEGER",
            "ALTER TABLE IF EXISTS \"courses\" ADD COLUMN IF NOT EXISTS total_sessions INTEGER",
            "ALTER TABLE IF EXISTS \"Courses\" ADD COLUMN IF NOT EXISTS total_sessions INTEGER",
            "ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS auto_generate_meet_link BOOLEAN DEFAULT false",
            "ALTER TABLE IF EXISTS \"courses\" ADD COLUMN IF NOT EXISTS auto_generate_meet_link BOOLEAN DEFAULT false",
            "ALTER TABLE IF EXISTS \"Courses\" ADD COLUMN IF NOT EXISTS auto_generate_meet_link BOOLEAN DEFAULT false",

            // Batches table variations
            "ALTER TABLE IF EXISTS \"Batches\" ADD COLUMN IF NOT EXISTS covered_topics TEXT",
            "ALTER TABLE IF EXISTS batches ADD COLUMN IF NOT EXISTS covered_topics TEXT",
            "ALTER TABLE IF EXISTS \"batches\" ADD COLUMN IF NOT EXISTS covered_topics TEXT",
            "ALTER TABLE IF EXISTS \"Batches\" ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255)",
            "ALTER TABLE IF EXISTS batches ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"batches\" ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"Batches\" ADD COLUMN IF NOT EXISTS class_timings VARCHAR(255)",
            "ALTER TABLE IF EXISTS batches ADD COLUMN IF NOT EXISTS class_timings VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"batches\" ADD COLUMN IF NOT EXISTS class_timings VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"Batches\" ADD COLUMN IF NOT EXISTS duration VARCHAR(255)",
            "ALTER TABLE IF EXISTS batches ADD COLUMN IF NOT EXISTS duration VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"batches\" ADD COLUMN IF NOT EXISTS duration VARCHAR(255)",

            // Teachers table variations
            "ALTER TABLE IF EXISTS teachers ADD COLUMN IF NOT EXISTS profile_image VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"teachers\" ADD COLUMN IF NOT EXISTS profile_image VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"Teachers\" ADD COLUMN IF NOT EXISTS profile_image VARCHAR(255)",

            // Class recording & student material
            "ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS file_url VARCHAR(1000)",
            "ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS file_size BIGINT",
            "ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS file_type VARCHAR(100)",
            "ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS uploaded_by_email VARCHAR(255)",
            "ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS uploaded_by_role VARCHAR(50)",
            "ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP",
            "ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS file_url VARCHAR(1000)",
            "ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS module_name VARCHAR(255)",
            "ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS file_type VARCHAR(100)",
            "ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS uploaded_by_email VARCHAR(255)",
            "ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS uploaded_by_role VARCHAR(50)",
            "ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP"
        );

        for (String sql : ddlStatements) {
            try {
                jdbcTemplate.execute(sql);
                log.info("Successfully executed schema repair: {}", sql);
            } catch (Exception e) {
                log.warn("Schema repair statement '{}' skipped or failed: {}", sql, e.getMessage());
            }
        }

        log.info("Automated database schema repairs completed successfully.");
    }
}
