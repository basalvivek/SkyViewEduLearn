-- ============================================================
-- Schedule Teacher Assignment
-- Run AFTER sql/11-drag-drop-recurring-schema.sql
-- ============================================================

-- 1. Add assigned_to column (nullable — null means admin-managed / not yet assigned)
ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS assigned_to UUID REFERENCES users(id);

-- 2. Index for fast per-teacher calendar queries
CREATE INDEX IF NOT EXISTS idx_sched_assigned_to
    ON exam_schedules(assigned_to)
    WHERE is_deleted = FALSE;
