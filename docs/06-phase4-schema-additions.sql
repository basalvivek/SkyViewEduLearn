-- ============================================================
-- EduLearn Platform — Phase 4 Schema Additions
-- Run AFTER sql/05-question-answer-corrections.sql
-- ============================================================

-- Module 13: Topic-score convenience view
CREATE OR REPLACE VIEW attempt_topic_scores AS
SELECT
    aa.attempt_id,
    t.id          AS topic_id,
    t.name        AS topic_name,
    s.name        AS subject_name,
    COUNT(aa.id)  AS question_count,
    SUM(COALESCE(eq.marks_override, q.marks)) AS total_marks,
    SUM(COALESCE(aa.marks_awarded, 0))         AS marks_obtained
FROM attempt_answers aa
JOIN exam_questions  eq ON eq.id = aa.exam_question_id
JOIN questions        q ON q.id  = aa.question_id
JOIN topics           t ON t.id  = q.topic_id
JOIN subjects         s ON s.id  = t.subject_id
GROUP BY aa.attempt_id, t.id, t.name, s.name;

-- Module 16: Question review flag columns
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS needs_review  BOOLEAN     NOT NULL DEFAULT FALSE;
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS review_reason TEXT;
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS flagged_at    TIMESTAMPTZ;
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS flagged_by    UUID REFERENCES users(id);

-- Module 16: Question analytics view
CREATE OR REPLACE VIEW question_analytics AS
SELECT
    q.id                                        AS question_id,
    q.question_text,
    q.question_type,
    q.marks                                     AS max_marks,
    q.needs_review,
    t.name                                      AS topic_name,
    s.name                                      AS subject_name,
    COUNT(aa.id)                                AS times_attempted,
    COUNT(aa.id) FILTER (WHERE aa.is_correct = TRUE)          AS correct_count,
    COUNT(aa.id) FILTER (WHERE aa.text_answer IS NULL
        AND aa.selected_option_ids IS NULL
        AND aa.boolean_answer IS NULL)                         AS skip_count,
    ROUND(AVG(COALESCE(aa.marks_awarded,0))::NUMERIC,2)       AS avg_marks_awarded,
    CASE WHEN COUNT(aa.id) > 0
        THEN ROUND(100.0 *
             COUNT(aa.id) FILTER (WHERE aa.is_correct = TRUE)
             / COUNT(aa.id), 1)
        ELSE NULL END                                          AS correct_pct
FROM questions q
LEFT JOIN attempt_answers aa ON aa.question_id = q.id
JOIN topics   t ON t.id = q.topic_id
JOIN subjects s ON s.id = t.subject_id
WHERE q.is_deleted = FALSE
GROUP BY q.id, q.question_text, q.question_type, q.marks,
         q.needs_review, t.name, s.name;
