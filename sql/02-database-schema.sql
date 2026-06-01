-- ============================================================
-- EduLearn Platform — PostgreSQL Schema v1.1
-- Hosted: localhost:5432/edulearn
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ENUMS
-- ============================================================
CREATE TYPE user_role      AS ENUM ('ADMIN', 'TEACHER', 'STUDENT');
CREATE TYPE content_status AS ENUM ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE question_type  AS ENUM (
    'MCQ_SINGLE','MCQ_MULTIPLE','TRUE_FALSE',
    'SHORT_ANSWER','ESSAY','CODE','IMAGE_BASED'
);
CREATE TYPE code_language  AS ENUM (
    'PYTHON','JAVA','JAVASCRIPT','SQL','HTML','CSS','OTHER'
);

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(200) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          user_role    NOT NULL DEFAULT 'TEACHER',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email ON users(email) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_role  ON users(role)  WHERE is_deleted = FALSE;

-- ============================================================
-- CATEGORIES  (e.g. Year 10)
-- ============================================================
CREATE TABLE categories (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(120)   NOT NULL,
    description      TEXT,
    status           content_status NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    created_by       UUID           NOT NULL REFERENCES users(id),
    updated_by       UUID           REFERENCES users(id),
    approved_by      UUID           REFERENCES users(id),
    approved_at      TIMESTAMPTZ,
    is_deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cat_status     ON categories(status)     WHERE is_deleted = FALSE;
CREATE INDEX idx_cat_created_by ON categories(created_by) WHERE is_deleted = FALSE;

-- ============================================================
-- SUBJECTS  (e.g. English Language)
-- ============================================================
CREATE TABLE subjects (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id      UUID           NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    name             VARCHAR(120)   NOT NULL,
    curriculum_level VARCHAR(50),
    description      TEXT,
    status           content_status NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    created_by       UUID           NOT NULL REFERENCES users(id),
    updated_by       UUID           REFERENCES users(id),
    approved_by      UUID           REFERENCES users(id),
    approved_at      TIMESTAMPTZ,
    is_deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sub_category_id ON subjects(category_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_sub_status      ON subjects(status)      WHERE is_deleted = FALSE;

-- ============================================================
-- TOPICS  (e.g. Non-Fiction & Creative Writing)
-- ============================================================
CREATE TABLE topics (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id       UUID           NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    name             VARCHAR(200)   NOT NULL,
    learning_objective TEXT,
    difficulty       VARCHAR(30)    CHECK (difficulty IN ('Foundation','Intermediate','Higher')),
    status           content_status NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    created_by       UUID           NOT NULL REFERENCES users(id),
    updated_by       UUID           REFERENCES users(id),
    approved_by      UUID           REFERENCES users(id),
    approved_at      TIMESTAMPTZ,
    is_deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_top_subject_id ON topics(subject_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_top_status     ON topics(status)     WHERE is_deleted = FALSE;

-- ============================================================
-- QUESTIONS
-- ============================================================
CREATE TABLE questions (
    id               UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id         UUID            NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    question_text    TEXT            NOT NULL,
    question_type    question_type   NOT NULL,
    marks            SMALLINT        NOT NULL DEFAULT 1 CHECK (marks > 0),
    answer_guide     TEXT,
    -- TRUE_FALSE
    correct_boolean  BOOLEAN,
    -- SHORT_ANSWER
    model_answer     TEXT,
    -- CODE
    code_lang        code_language,
    starter_code     TEXT,
    expected_output  TEXT,
    -- IMAGE_BASED
    image_url        VARCHAR(500),
    image_alt_text   VARCHAR(255),
    status           content_status  NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    created_by       UUID            NOT NULL REFERENCES users(id),
    updated_by       UUID            REFERENCES users(id),
    approved_by      UUID            REFERENCES users(id),
    approved_at      TIMESTAMPTZ,
    is_deleted       BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_q_topic_id  ON questions(topic_id)      WHERE is_deleted = FALSE;
CREATE INDEX idx_q_type      ON questions(question_type)  WHERE is_deleted = FALSE;
CREATE INDEX idx_q_status    ON questions(status)         WHERE is_deleted = FALSE;
CREATE INDEX idx_q_created_by ON questions(created_by)   WHERE is_deleted = FALSE;

-- ============================================================
-- QUESTION OPTIONS  (MCQ choices)
-- ============================================================
CREATE TABLE question_options (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id   UUID        NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_text   TEXT        NOT NULL,
    is_correct    BOOLEAN     NOT NULL DEFAULT FALSE,
    display_order SMALLINT    NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_opt_question_id ON question_options(question_id);

-- ============================================================
-- APPROVAL AUDIT LOG
-- ============================================================
CREATE TABLE approval_log (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(20)    NOT NULL CHECK (entity_type IN ('CATEGORY','SUBJECT','TOPIC','QUESTION')),
    entity_id   UUID           NOT NULL,
    from_status content_status NOT NULL,
    to_status   content_status NOT NULL,
    actioned_by UUID           NOT NULL REFERENCES users(id),
    note        TEXT,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_alog_entity ON approval_log(entity_type, entity_id);
CREATE INDEX idx_alog_actor  ON approval_log(actioned_by);

-- ============================================================
-- AUTO updated_at trigger
-- ============================================================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_upd      BEFORE UPDATE ON users      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_cat_upd        BEFORE UPDATE ON categories  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_sub_upd        BEFORE UPDATE ON subjects    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_top_upd        BEFORE UPDATE ON topics      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_q_upd          BEFORE UPDATE ON questions   FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- SEED: default admin  (password: Admin@123 — replace hash!)
-- Generate with: htpasswd -bnBC 12 "" Admin@123 | tr -d ':\n'
-- ============================================================
INSERT INTO users (full_name, email, password_hash, role)
VALUES ('Platform Admin', 'admin@localhost',
        '$2a$12$REPLACE_WITH_REAL_BCRYPT_HASH_HERE_admin123', 'ADMIN');
