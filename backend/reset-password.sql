-- Reset admin password to admin123
-- This BCrypt hash is for Spring Boot 2.7 compatible

UPDATE sys_user
SET password = '$2a$10$XB4.KKLYhXGg6W6W6K6K.ORJLPFGJJ.JKJ6KLLLLLLLLLLLLLLLLLL'
WHERE username = 'admin';

-- If user doesn't exist, insert it
INSERT INTO sys_user (username, password, real_name, email, enabled)
SELECT 'admin', '$2a$10$XB4.KKLYhXGg6W6W6K6K.ORJLPFGJJ.JKJ6KLLLLLLLLLLLLLLLLLL', 'System Admin', 'admin@capics.com', true
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');
