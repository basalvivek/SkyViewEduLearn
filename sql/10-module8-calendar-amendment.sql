-- ============================================================
-- Module 8 Calendar Amendment
-- Run AFTER sql/04-phase3-schema-additions.sql
-- ============================================================

-- 1. Create schedule_type enum
DO $$ BEGIN
  CREATE TYPE schedule_type AS ENUM ('EXAM', 'CLASS', 'EVENT', 'HOLIDAY');
EXCEPTION WHEN duplicate_object THEN null;
END $$;

-- 2. Add title column (used for non-exam events; optional for EXAM type)
ALTER TABLE exam_schedules ADD COLUMN IF NOT EXISTS title VARCHAR(200);

-- 3. Make exam_id nullable (CLASS/EVENT/HOLIDAY have no exam)
ALTER TABLE exam_schedules ALTER COLUMN exam_id DROP NOT NULL;

-- 4. Make class_id nullable (HOLIDAY may span all classes)
ALTER TABLE exam_schedules ALTER COLUMN class_id DROP NOT NULL;

-- 5. Add schedule_type column, default EXAM (preserves existing rows)
ALTER TABLE exam_schedules ADD COLUMN IF NOT EXISTS schedule_type schedule_type NOT NULL DEFAULT 'EXAM';

-- 6. Date-range index for fast calendar feed queries
CREATE INDEX IF NOT EXISTS idx_sched_start_at
    ON exam_schedules(start_at)
    WHERE is_deleted = FALSE;
