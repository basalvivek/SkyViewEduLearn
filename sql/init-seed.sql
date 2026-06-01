-- Fix admin password (replace placeholder with real BCrypt hash via pgcrypto)
UPDATE users
SET password_hash = crypt('Admin@123', gen_salt('bf', 12))
WHERE email = 'admin@localhost';

-- Default teacher account
INSERT INTO users (full_name, email, password_hash, role)
SELECT 'Demo Teacher', 'teacher@localhost', crypt('Teacher@123', gen_salt('bf', 12)), 'TEACHER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'teacher@localhost');
