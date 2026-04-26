INSERT INTO sys_role (role_code, role_name, description)
SELECT 'PLAN', 'Planning', 'Planning user with all features except system config'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_code = 'PLAN'
);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'PLAN'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_role ur
    WHERE ur.user_id = u.id
);

INSERT INTO sys_role (role_code, role_name, description)
SELECT 'MASTERDATA', 'Master Data', 'Master data user with planning permissions and master-data maintenance rights'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_code = 'MASTERDATA'
);
