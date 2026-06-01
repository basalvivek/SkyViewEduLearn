-- ============================================================
-- EduLearn Platform — Phase 5 Schema Additions
-- Run AFTER sql/06-phase4-schema-additions.sql
-- ============================================================

-- Module 17: Notifications
CREATE TYPE notification_type AS ENUM (
    'CONTENT_SUBMITTED','CONTENT_APPROVED','CONTENT_REJECTED',
    'EXAM_SUBMITTED','EXAM_APPROVED','EXAM_REJECTED',
    'EXAM_SCHEDULED','EXAM_REMINDER','RESULTS_RELEASED',
    'MARKING_COMPLETE','ACCOUNT_CREATED','PASSWORD_RESET',
    'QUESTION_FLAGGED','ANNOUNCEMENT'
);

CREATE TABLE notifications (
    id          UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        notification_type NOT NULL,
    title       VARCHAR(200)      NOT NULL,
    message     TEXT              NOT NULL,
    link        VARCHAR(300),
    entity_type VARCHAR(20),
    entity_id   UUID,
    is_read     BOOLEAN           NOT NULL DEFAULT FALSE,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_user_unread  ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notif_user_created ON notifications(user_id, created_at DESC);

-- Module 18: Announcements
CREATE TYPE announcement_audience AS ENUM (
    'ALL_TEACHERS','ALL_STUDENTS','SPECIFIC_CLASS','EVERYONE'
);
CREATE TYPE announcement_priority AS ENUM ('NORMAL','IMPORTANT','URGENT');

CREATE TABLE announcements (
    id               UUID                  PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(200)          NOT NULL,
    body             TEXT                  NOT NULL,
    audience         announcement_audience NOT NULL,
    class_id         UUID                  REFERENCES classes(id),
    priority         announcement_priority NOT NULL DEFAULT 'NORMAL',
    pin_to_dashboard BOOLEAN               NOT NULL DEFAULT FALSE,
    publish_at       TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    expires_at       TIMESTAMPTZ,
    is_deleted       BOOLEAN               NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_by       UUID                  NOT NULL REFERENCES users(id),
    created_at       TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ           NOT NULL DEFAULT NOW()
);

CREATE TABLE announcement_dismissals (
    announcement_id UUID NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dismissed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (announcement_id, user_id)
);

CREATE INDEX idx_ann_audience   ON announcements(audience)         WHERE is_deleted = FALSE;
CREATE INDEX idx_ann_class      ON announcements(class_id)         WHERE is_deleted = FALSE;
CREATE INDEX idx_ann_publish    ON announcements(publish_at)       WHERE is_deleted = FALSE;
CREATE INDEX idx_ann_pinned     ON announcements(pin_to_dashboard) WHERE pin_to_dashboard = TRUE AND is_deleted = FALSE;

CREATE TRIGGER trg_ann_upd
    BEFORE UPDATE ON announcements FOR EACH ROW EXECUTE FUNCTION set_updated_at();
