-- ============================================================
-- EduLearn Platform — Drag-Drop & Recurring Schedules Schema
-- Run AFTER sql/10-module8-calendar-amendment.sql
-- ============================================================

-- Recurrence pattern type
CREATE TYPE recurrence_type AS ENUM (
    'NONE','DAILY','WEEKLY','FORTNIGHTLY','MONTHLY'
);

-- Scope for series move operations (used by service layer, not stored)
-- CREATE TYPE series_move_scope AS ENUM ('THIS','THIS_AND_FORWARD','ALL');

-- ── Series tracking ───────────────────────────────────────────
-- Groups recurring / range events into a named series
ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS series_id UUID;

-- Recurrence pattern stored for display and re-edit
ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS recurrence_type recurrence_type NOT NULL DEFAULT 'NONE';

-- Days of week as comma-separated ints: Mon=1 … Sun=7
-- e.g. "1,3,5" = Mon, Wed, Fri
ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS recurrence_days VARCHAR(20);

-- Window of the overall series (for display in series detail panel)
ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS series_start DATE;

ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS series_end DATE;

-- ── Indexes ───────────────────────────────────────────────────

-- Fast lookup of all events in a series (for move/delete series operations)
CREATE INDEX IF NOT EXISTS idx_sched_series_id
    ON exam_schedules(series_id)
    WHERE series_id IS NOT NULL AND is_deleted = FALSE;

-- Fast forward-slice for "this and following" move scope
CREATE INDEX IF NOT EXISTS idx_sched_series_start_at
    ON exam_schedules(series_id, start_at)
    WHERE series_id IS NOT NULL AND is_deleted = FALSE;

-- ── Verification ──────────────────────────────────────────────
-- SELECT column_name, data_type
-- FROM information_schema.columns
-- WHERE table_name = 'exam_schedules'
-- AND column_name IN (
--     'series_id','recurrence_type','recurrence_days','series_start','series_end'
-- );
