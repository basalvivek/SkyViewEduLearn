-- ============================================================
-- EduLearn Platform — Phase 2 Schema Additions
-- Run AFTER sql/02-database-schema.sql
-- ============================================================

-- ── Module 4: Profile fields on users ────────────────────────
ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name   VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_path    VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS job_title      VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS department     VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio            TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone          VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at  TIMESTAMPTZ;

-- ── Module 5: Submission unified view ────────────────────────
-- Makes querying all content types by teacher simpler
CREATE OR REPLACE VIEW teacher_submissions AS
  SELECT
    'CATEGORY'                    AS entity_type,
    id                            AS entity_id,
    name                          AS label,
    status,
    rejection_reason,
    created_by,
    updated_by,
    approved_by,
    approved_at,
    created_at,
    updated_at
  FROM categories WHERE is_deleted = FALSE

UNION ALL
  SELECT
    'SUBJECT',
    id,
    name,
    status,
    rejection_reason,
    created_by,
    updated_by,
    approved_by,
    approved_at,
    created_at,
    updated_at
  FROM subjects WHERE is_deleted = FALSE

UNION ALL
  SELECT
    'TOPIC',
    id,
    name,
    status,
    rejection_reason,
    created_by,
    updated_by,
    approved_by,
    approved_at,
    created_at,
    updated_at
  FROM topics WHERE is_deleted = FALSE

UNION ALL
  SELECT
    'QUESTION',
    id,
    LEFT(question_text, 120)      AS label,
    status,
    rejection_reason,
    created_by,
    updated_by,
    approved_by,
    approved_at,
    created_at,
    updated_at
  FROM questions WHERE is_deleted = FALSE;

-- ── Indexes to support submission queries ─────────────────────
-- These speed up "all content by teacher" queries across tables
CREATE INDEX IF NOT EXISTS idx_cat_created_by_status ON categories(created_by, status) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_sub_created_by_status ON subjects(created_by,   status) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_top_created_by_status ON topics(created_by,     status) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_q_created_by_status   ON questions(created_by,  status) WHERE is_deleted = FALSE;
