ALTER TABLE enquiries ADD COLUMN IF NOT EXISTS source VARCHAR(100);
ALTER TABLE enquiries ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE enquiries ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT false;
UPDATE enquiries SET is_read = false WHERE is_read IS NULL;

ALTER TABLE security_settings ADD COLUMN IF NOT EXISTS activity_status_enabled BOOLEAN DEFAULT true;
ALTER TABLE security_settings ADD COLUMN IF NOT EXISTS online BOOLEAN DEFAULT false;
UPDATE security_settings SET activity_status_enabled = true WHERE activity_status_enabled IS NULL;
UPDATE security_settings SET online = false WHERE online IS NULL;

-- Expand courses columns to TEXT for multi-module syllabus support
ALTER TABLE courses ALTER COLUMN syllabus_topics TYPE TEXT;
ALTER TABLE courses ALTER COLUMN what_you_will_learn TYPE TEXT;
ALTER TABLE courses ALTER COLUMN description TYPE TEXT;
