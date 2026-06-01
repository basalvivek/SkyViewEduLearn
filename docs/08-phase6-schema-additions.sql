-- ============================================================
-- EduLearn Platform — Phase 6 Schema Additions
-- Run AFTER sql/07-phase5-schema-additions.sql
-- ============================================================

-- Module 19: Full audit log (immutable — no updates or deletes)
CREATE TYPE audit_action AS ENUM (
    'CONTENT_CREATED','CONTENT_UPDATED','CONTENT_SUBMITTED',
    'CONTENT_APPROVED','CONTENT_REJECTED','CONTENT_DELETED',
    'EXAM_CREATED','EXAM_UPDATED','EXAM_SUBMITTED',
    'EXAM_APPROVED','EXAM_REJECTED','EXAM_DELETED',
    'EXAM_SCHEDULED','EXAM_SCHEDULE_CANCELLED',
    'USER_CREATED','USER_UPDATED','USER_DEACTIVATED',
    'USER_PASSWORD_RESET','USER_LOGIN','USER_LOGOUT',
    'QUESTION_FLAGGED','QUESTION_UNFLAGGED',
    'SETTINGS_CHANGED','ORG_SETTINGS_UPDATED',
    'ANNOUNCEMENT_CREATED','ANNOUNCEMENT_DELETED'
);

CREATE TABLE audit_log (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    action      audit_action NOT NULL,
    entity_type VARCHAR(30),
    entity_id   UUID,
    entity_name VARCHAR(300),
    actor_id    UUID         NOT NULL REFERENCES users(id),
    actor_name  VARCHAR(120) NOT NULL,
    actor_role  user_role    NOT NULL,
    detail      JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alog_actor      ON audit_log(actor_id);
CREATE INDEX idx_alog_action     ON audit_log(action);
CREATE INDEX idx_alog_entity     ON audit_log(entity_type, entity_id);
CREATE INDEX idx_alog_created_at ON audit_log(created_at DESC);

-- Module 21: Platform settings (key-value store with typed values)
CREATE TABLE platform_settings (
    key         VARCHAR(100) PRIMARY KEY,
    value       TEXT         NOT NULL,
    value_type  VARCHAR(20)  NOT NULL CHECK (value_type IN ('STRING','INTEGER','BOOLEAN','DATE')),
    category    VARCHAR(50)  NOT NULL,
    label       VARCHAR(200) NOT NULL,
    description TEXT,
    updated_by  UUID         REFERENCES users(id),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO platform_settings (key,value,value_type,category,label) VALUES
('term.name',             'Autumn 2025','STRING', 'academic','Current term name'),
('term.start_date',       '',           'DATE',   'academic','Term start date'),
('term.end_date',         '',           'DATE',   'academic','Term end date'),
('exam.default_time',     '60',         'INTEGER','exam',    'Default time limit (minutes)'),
('exam.default_pass_pct', '50',         'INTEGER','exam',    'Default pass mark (%)'),
('exam.max_attempts',     '1',          'INTEGER','exam',    'Max attempts per schedule'),
('exam.allow_resits',     'true',       'BOOLEAN','exam',    'Allow exam re-sits'),
('exam.auto_release',     'false',      'BOOLEAN','exam',    'Auto-release results on submit'),
('security.session_mins', '30',         'INTEGER','security','Session timeout (minutes)'),
('security.min_password', '8',          'INTEGER','security','Minimum password length'),
('security.req_upper',    'true',       'BOOLEAN','security','Require uppercase letter'),
('security.req_number',   'true',       'BOOLEAN','security','Require number'),
('security.req_special',  'false',      'BOOLEAN','security','Require special character'),
('security.max_attempts', '5',          'INTEGER','security','Max login attempts'),
('security.lockout_mins', '15',         'INTEGER','security','Lockout duration (minutes)'),
('approval.admin_auto',   'true',       'BOOLEAN','approval','Auto-approve admin content'),
('approval.notify_all',   'true',       'BOOLEAN','approval','Notify all admins on submission');
