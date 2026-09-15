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

            // Enrollments batch columns
            "ALTER TABLE IF EXISTS enrollments ADD COLUMN IF NOT EXISTS batch_name VARCHAR(255)",
            "ALTER TABLE IF EXISTS enrollments ADD COLUMN IF NOT EXISTS batch_id BIGINT",
            "ALTER TABLE IF EXISTS \"enrollments\" ADD COLUMN IF NOT EXISTS batch_name VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"enrollments\" ADD COLUMN IF NOT EXISTS batch_id BIGINT",
            "ALTER TABLE IF EXISTS \"Enrollments\" ADD COLUMN IF NOT EXISTS batch_name VARCHAR(255)",
            "ALTER TABLE IF EXISTS \"Enrollments\" ADD COLUMN IF NOT EXISTS batch_id BIGINT",

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

            // Batches table creation & variations
            "CREATE TABLE IF NOT EXISTS batches (id BIGSERIAL PRIMARY KEY, batch_name VARCHAR(255), select_course VARCHAR(255), instructor VARCHAR(255), start_date DATE, end_date DATE, status VARCHAR(50), created_at TIMESTAMP, class_timings VARCHAR(255), duration VARCHAR(255), google_meet_link VARCHAR(255), covered_topics TEXT)",
            "CREATE TABLE IF NOT EXISTS batch_class_days (batch_id BIGINT, class_days VARCHAR(50))",
            "INSERT INTO batches (id, batch_name, select_course, instructor, start_date, end_date, status, created_at, class_timings, duration, google_meet_link, covered_topics) SELECT id, batch_name, select_course, instructor, start_date, end_date, status, created_at, class_timings, duration, google_meet_link, covered_topics FROM \"Batches\" ON CONFLICT (id) DO NOTHING",
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
            "ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP",

            // Sync missing student enrollments from students.course
            "INSERT INTO enrollments (student_id, course_title, enrollment_date, payment_status) SELECT s.id, TRIM(s.course), COALESCE(s.enrollment_date, '2026-06-01'), COALESCE(s.payment_status, 'PAID') FROM students s WHERE s.course IS NOT NULL AND TRIM(s.course) != '' AND NOT EXISTS (SELECT 1 FROM enrollments e WHERE e.student_id = s.id AND LOWER(TRIM(e.course_title)) = LOWER(TRIM(s.course)))",

            // Delete duplicate enrollments keeping the one with batch assigned or lowest ID
            "DELETE FROM enrollments e1 WHERE e1.id IN (SELECT e_dup.id FROM enrollments e_dup JOIN enrollments e_keep ON e_dup.student_id = e_keep.student_id AND LOWER(TRIM(e_dup.course_title)) = LOWER(TRIM(e_keep.course_title)) AND e_dup.id > e_keep.id WHERE (e_keep.batch_name IS NOT NULL AND e_dup.batch_name IS NULL) OR (e_keep.batch_name IS NOT NULL AND e_dup.batch_name IS NOT NULL) OR (e_keep.batch_name IS NULL AND e_dup.batch_name IS NULL))"
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
