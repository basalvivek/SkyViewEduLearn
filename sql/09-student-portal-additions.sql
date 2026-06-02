-- Phase: Student Portal additions
-- Adds must_change_password flag to users table

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_users_must_change
    ON users(must_change_password) WHERE must_change_password = TRUE;
