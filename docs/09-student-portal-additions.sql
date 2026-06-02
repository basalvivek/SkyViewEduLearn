-- ============================================================
-- EduLearn Platform — Student Portal Additions
-- Run AFTER sql/05-question-answer-corrections.sql
-- BEFORE sql/06-phase4-schema-additions.sql
-- ============================================================

-- Fix 2: First-login forced password change flag
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Index to speed up auth check on login
CREATE INDEX IF NOT EXISTS idx_users_must_change
    ON users(must_change_password)
    WHERE must_change_password = TRUE;

-- ── Verification ──────────────────────────────────────────────
-- SELECT column_name, data_type, column_default
-- FROM information_schema.columns
-- WHERE table_name = 'users' AND column_name = 'must_change_password';
