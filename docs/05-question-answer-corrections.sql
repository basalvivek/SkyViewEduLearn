-- ============================================================
-- EduLearn Platform — Question Answer Capture Corrections
-- Run AFTER sql/04-phase3-schema-additions.sql
-- Fixes gaps in question builder and exam attempt answer storage
-- ============================================================

-- ── questions table: missing answer-capture columns ───────────

-- Shown to student after completing the attempt (all types)
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS answer_explanation TEXT;

-- ESSAY: marking criteria breakdown shown to marker
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS marking_scheme TEXT;

-- ESSAY: optional word cap (NULL = no limit)
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS word_limit INTEGER
        CONSTRAINT chk_word_limit CHECK (word_limit IS NULL OR word_limit > 0);

-- IMAGE_BASED: controls whether student writes text or picks MCQ option
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS image_answer_type VARCHAR(10)
        CONSTRAINT chk_img_answer_type CHECK (image_answer_type IN ('WRITTEN','MCQ'));

-- ── question_options: partial marks per option (MCQ_MULTIPLE) ──
-- NULL  = use the exam-level all-or-nothing strategy
-- Value = exact marks awarded when this option correctly selected/avoided
ALTER TABLE question_options
    ADD COLUMN IF NOT EXISTS partial_marks SMALLINT DEFAULT NULL
        CONSTRAINT chk_partial_marks CHECK (partial_marks IS NULL OR partial_marks >= 0);

-- ── exam_questions: partial marking flag per question in exam ──
-- FALSE (default) = full marks only for MCQ_MULTIPLE (exact match required)
-- TRUE            = award marks per correctly selected option
ALTER TABLE exam_questions
    ADD COLUMN IF NOT EXISTS partial_marking BOOLEAN NOT NULL DEFAULT FALSE;

-- ── Verification queries (run to confirm) ─────────────────────
-- SELECT column_name, data_type FROM information_schema.columns
-- WHERE table_name = 'questions'
-- AND column_name IN ('answer_explanation','marking_scheme','word_limit','image_answer_type');

-- SELECT column_name FROM information_schema.columns
-- WHERE table_name = 'question_options' AND column_name = 'partial_marks';

-- SELECT column_name FROM information_schema.columns
-- WHERE table_name = 'exam_questions' AND column_name = 'partial_marking';
