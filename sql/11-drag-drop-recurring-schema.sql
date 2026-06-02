-- ============================================================
-- Drag-Drop & Recurring Schedules Schema
-- Run AFTER sql/10-module8-calendar-amendment.sql
-- ============================================================

-- 1. Create recurrence_type enum
DO $$ BEGIN
  CREATE TYPE recurrence_type AS ENUM ('NONE', 'DAILY', 'WEEKLY', 'FORTNIGHTLY', 'MONTHLY');
EXCEPTION WHEN duplicate_object THEN null;
END $$;

-- 2. Series ID — groups all events in a recurring/range series
ALTER TABLE exam_schedules ADD COLUMN IF NOT EXISTS series_id UUID;

-- 3. Recurrence metadata
ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS recurrence_type recurrence_type DEFAULT 'NONE';

-- Comma-separated day numbers: "1,2,3,4,5" = Mon-Fri (Mon=1, Sun=7)
ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS recurrence_days VARCHAR(20);

ALTER TABLE exam_schedules ADD COLUMN IF NOT EXISTS series_start DATE;
ALTER TABLE exam_schedules ADD COLUMN IF NOT EXISTS series_end   DATE;

-- 4. Indexes for series queries
CREATE INDEX IF NOT EXISTS idx_sched_series_id
    ON exam_schedules(series_id)
    WHERE series_id IS NOT NULL AND is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_sched_start_range
    ON exam_schedules(start_at, end_at)
    WHERE is_deleted = FALSE;
