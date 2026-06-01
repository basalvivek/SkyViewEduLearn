-- Phase correction: question type-specific answer capture fields
-- Run this against your PostgreSQL database before restarting the app.

-- question_options: partial marks support for MCQ_MULTIPLE
ALTER TABLE question_options
    ADD COLUMN IF NOT EXISTS partial_marks SMALLINT DEFAULT NULL;

-- questions: answer explanation shown to student after attempt
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS answer_explanation TEXT;

-- questions: essay marking criteria
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS marking_scheme TEXT;

-- questions: essay word cap (NULL = no limit)
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS word_limit INTEGER;

-- questions: image-based question answer subtype
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS image_answer_type VARCHAR(10)
        CHECK (image_answer_type IN ('WRITTEN', 'MCQ'));

-- exam_questions: partial marking flag (used by AutoMarkingService in Phase 3)
ALTER TABLE exam_questions
    ADD COLUMN IF NOT EXISTS partial_marking BOOLEAN NOT NULL DEFAULT FALSE;
