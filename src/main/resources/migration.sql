ALTER TABLE IF EXISTS enquiries ADD COLUMN IF NOT EXISTS source VARCHAR(100);
ALTER TABLE IF EXISTS enquiries ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE IF EXISTS enquiries ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT false;
UPDATE enquiries SET is_read = false WHERE is_read IS NULL;

ALTER TABLE IF EXISTS security_settings ADD COLUMN IF NOT EXISTS activity_status_enabled BOOLEAN DEFAULT true;
ALTER TABLE IF EXISTS security_settings ADD COLUMN IF NOT EXISTS online BOOLEAN DEFAULT false;
UPDATE security_settings SET activity_status_enabled = true WHERE activity_status_enabled IS NULL;
UPDATE security_settings SET online = false WHERE online IS NULL;

-- Expand courses columns to TEXT for multi-module syllabus support
ALTER TABLE IF EXISTS courses ALTER COLUMN syllabus_topics TYPE TEXT;
ALTER TABLE IF EXISTS courses ALTER COLUMN what_you_will_learn TYPE TEXT;
ALTER TABLE IF EXISTS courses ALTER COLUMN description TYPE TEXT;

-- Covered topics columns on courses and batches
ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS covered_topics TEXT;
ALTER TABLE IF EXISTS "courses" ADD COLUMN IF NOT EXISTS covered_topics TEXT;
ALTER TABLE IF EXISTS "Courses" ADD COLUMN IF NOT EXISTS covered_topics TEXT;
ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255);
ALTER TABLE IF EXISTS "courses" ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255);
ALTER TABLE IF EXISTS "Courses" ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255);
ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS meet_link VARCHAR(255);
ALTER TABLE IF EXISTS "courses" ADD COLUMN IF NOT EXISTS meet_link VARCHAR(255);
ALTER TABLE IF EXISTS "Courses" ADD COLUMN IF NOT EXISTS meet_link VARCHAR(255);

ALTER TABLE IF EXISTS "Batches" ADD COLUMN IF NOT EXISTS covered_topics TEXT;
ALTER TABLE IF EXISTS batches ADD COLUMN IF NOT EXISTS covered_topics TEXT;
ALTER TABLE IF EXISTS "batches" ADD COLUMN IF NOT EXISTS covered_topics TEXT;

ALTER TABLE IF EXISTS "Batches" ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255);
ALTER TABLE IF EXISTS batches ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255);
ALTER TABLE IF EXISTS "batches" ADD COLUMN IF NOT EXISTS google_meet_link VARCHAR(255);

ALTER TABLE IF EXISTS "Batches" ADD COLUMN IF NOT EXISTS class_timings VARCHAR(255);
ALTER TABLE IF EXISTS batches ADD COLUMN IF NOT EXISTS class_timings VARCHAR(255);
ALTER TABLE IF EXISTS "batches" ADD COLUMN IF NOT EXISTS class_timings VARCHAR(255);

ALTER TABLE IF EXISTS "Batches" ADD COLUMN IF NOT EXISTS duration VARCHAR(255);
ALTER TABLE IF EXISTS batches ADD COLUMN IF NOT EXISTS duration VARCHAR(255);
ALTER TABLE IF EXISTS "batches" ADD COLUMN IF NOT EXISTS duration VARCHAR(255);

-- Teachers columns
ALTER TABLE IF EXISTS teachers ADD COLUMN IF NOT EXISTS profile_image VARCHAR(255);
ALTER TABLE IF EXISTS "teachers" ADD COLUMN IF NOT EXISTS profile_image VARCHAR(255);
ALTER TABLE IF EXISTS "Teachers" ADD COLUMN IF NOT EXISTS profile_image VARCHAR(255);

ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS file_url VARCHAR(1000);
ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS file_size BIGINT;
ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS file_type VARCHAR(100);
ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS uploaded_by_email VARCHAR(255);
ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS uploaded_by_role VARCHAR(50);
ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP;
ALTER TABLE IF EXISTS class_recording ADD COLUMN IF NOT EXISTS file_data BYTEA;

ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS file_url VARCHAR(1000);
ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS module_name VARCHAR(255);
ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS file_type VARCHAR(100);
ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS uploaded_by_email VARCHAR(255);
ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS uploaded_by_role VARCHAR(50);
ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP;
ALTER TABLE IF EXISTS student_material ADD COLUMN IF NOT EXISTS file_data BYTEA;

-- Update orphan demo seed recordings without file_data to real video lecture URLs
UPDATE class_recording 
SET file_url = 'https://www.youtube.com/watch?v=nu_pCVPKzTk' 
WHERE (file_data IS NULL OR octet_length(file_data) = 0) 
  AND (file_url IS NULL OR file_url LIKE '%stream%') 
  AND (title ILIKE '%Full Stack%' OR title ILIKE '%Orientation%');

UPDATE class_recording 
SET file_url = 'https://www.youtube.com/watch?v=35EQXmHKZYs' 
WHERE (file_data IS NULL OR octet_length(file_data) = 0) 
  AND (file_url IS NULL OR file_url LIKE '%stream%') 
  AND (title ILIKE '%Spring Boot%' OR title ILIKE '%Java%');
