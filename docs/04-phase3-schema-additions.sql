-- ============================================================
-- EduLearn Platform — Phase 3 Schema Additions
-- Run AFTER sql/03-phase2-schema-additions.sql
-- ============================================================

-- ── Phase 3 Enums ─────────────────────────────────────────────
CREATE TYPE exam_status     AS ENUM ('DRAFT','PENDING','APPROVED','REJECTED','ARCHIVED');
CREATE TYPE schedule_status AS ENUM ('UPCOMING','ACTIVE','CLOSED','CANCELLED');
CREATE TYPE attempt_status  AS ENUM ('IN_PROGRESS','SUBMITTED','TIMED_OUT','ABANDONED');

-- ── Module 7: Exams ───────────────────────────────────────────
CREATE TABLE exams (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(200) NOT NULL,
    description       TEXT,
    time_limit_mins   SMALLINT    NOT NULL DEFAULT 60 CHECK (time_limit_mins > 0),
    total_marks       SMALLINT    NOT NULL DEFAULT 0,
    pass_mark         SMALLINT,
    shuffle_questions BOOLEAN     NOT NULL DEFAULT FALSE,
    shuffle_options   BOOLEAN     NOT NULL DEFAULT FALSE,
    status            exam_status NOT NULL DEFAULT 'DRAFT',
    rejection_reason  TEXT,
    created_by        UUID        NOT NULL REFERENCES users(id),
    updated_by        UUID        REFERENCES users(id),
    approved_by       UUID        REFERENCES users(id),
    approved_at       TIMESTAMPTZ,
    is_deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE exam_questions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         UUID        NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id     UUID        NOT NULL REFERENCES questions(id),
    display_order   SMALLINT    NOT NULL DEFAULT 0,
    marks_override  SMALLINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (exam_id, question_id)
);

CREATE INDEX idx_exam_status     ON exams(status)     WHERE is_deleted = FALSE;
CREATE INDEX idx_exam_created_by ON exams(created_by) WHERE is_deleted = FALSE;
CREATE INDEX idx_exam_q_exam_id  ON exam_questions(exam_id);

CREATE TRIGGER trg_exams_upd
    BEFORE UPDATE ON exams FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Module 8: Classes & Scheduling ───────────────────────────
CREATE TABLE classes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    year_group  VARCHAR(20),
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by  UUID        NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE exam_schedules (
    id                       UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id                  UUID            NOT NULL REFERENCES exams(id),
    class_id                 UUID            NOT NULL REFERENCES classes(id),
    start_at                 TIMESTAMPTZ     NOT NULL,
    end_at                   TIMESTAMPTZ     NOT NULL,
    max_attempts             SMALLINT        NOT NULL DEFAULT 1,
    show_results_immediately BOOLEAN         NOT NULL DEFAULT FALSE,
    instructions             TEXT,
    status                   schedule_status NOT NULL DEFAULT 'UPCOMING',
    created_by               UUID            NOT NULL REFERENCES users(id),
    updated_by               UUID            REFERENCES users(id),
    is_deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at               TIMESTAMPTZ,
    created_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_schedule_dates CHECK (end_at > start_at)
);

CREATE INDEX idx_sched_exam_id  ON exam_schedules(exam_id)  WHERE is_deleted = FALSE;
CREATE INDEX idx_sched_class_id ON exam_schedules(class_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_sched_status   ON exam_schedules(status)   WHERE is_deleted = FALSE;
CREATE INDEX idx_classes_year   ON classes(year_group)      WHERE is_active = TRUE;

CREATE TRIGGER trg_classes_upd
    BEFORE UPDATE ON classes FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_sched_upd
    BEFORE UPDATE ON exam_schedules FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Module 9: Student → Class assignments ────────────────────
CREATE TABLE student_classes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    class_id    UUID        NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (student_id, class_id)
);

CREATE INDEX idx_sc_student_id ON student_classes(student_id) WHERE is_active = TRUE;
CREATE INDEX idx_sc_class_id   ON student_classes(class_id)   WHERE is_active = TRUE;

-- ── Modules 10-12: Attempts & Answers ────────────────────────
CREATE TABLE exam_attempts (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id     UUID           NOT NULL REFERENCES exam_schedules(id),
    student_id      UUID           NOT NULL REFERENCES users(id),
    started_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    submitted_at    TIMESTAMPTZ,
    status          attempt_status NOT NULL DEFAULT 'IN_PROGRESS',
    total_marks     SMALLINT,
    marks_obtained  SMALLINT,
    percentage      NUMERIC(5,2),
    is_passed       BOOLEAN,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE (schedule_id, student_id)
);

CREATE TABLE attempt_answers (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id          UUID        NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    question_id         UUID        NOT NULL REFERENCES questions(id),
    exam_question_id    UUID        NOT NULL REFERENCES exam_questions(id),
    -- Per question-type answer storage
    selected_option_ids UUID[],     -- MCQ_SINGLE / MCQ_MULTIPLE
    boolean_answer      BOOLEAN,    -- TRUE_FALSE
    text_answer         TEXT,       -- SHORT_ANSWER / ESSAY / CODE / IMAGE_BASED
    -- Marking
    marks_awarded       SMALLINT,
    is_correct          BOOLEAN,
    is_flagged          BOOLEAN     NOT NULL DEFAULT FALSE,
    marker_note         TEXT,
    marked_by           UUID        REFERENCES users(id),
    marked_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_attempt_schedule  ON exam_attempts(schedule_id);
CREATE INDEX idx_attempt_student   ON exam_attempts(student_id);
CREATE INDEX idx_attempt_status    ON exam_attempts(status);
CREATE INDEX idx_answer_attempt_id ON attempt_answers(attempt_id);
CREATE INDEX idx_answer_marker     ON attempt_answers(marked_by) WHERE marked_by IS NOT NULL;

CREATE TRIGGER trg_attempts_upd
    BEFORE UPDATE ON exam_attempts FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_answers_upd
    BEFORE UPDATE ON attempt_answers FOR EACH ROW EXECUTE FUNCTION set_updated_at();
