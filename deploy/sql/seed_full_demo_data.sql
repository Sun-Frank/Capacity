-- CAPICS full demo seed (idempotent)
-- Scope: keeps system tables/users, refreshes business demo data only.

BEGIN;

-- 0) Clean previous demo rows
DELETE FROM simulation_snapshot WHERE file_name = 'CAPICS_DEMO_V1.xlsx';
DELETE FROM line_realtime WHERE mrp_version = 'DEMO_V1';
DELETE FROM mrp_plan WHERE file_name = 'CAPICS_DEMO_V1.xlsx';
DELETE FROM routing_item WHERE routing_id IN (
  SELECT id FROM routing WHERE product_number IN ('FG1001', 'FG1002')
);
DELETE FROM routing WHERE product_number IN ('FG1001', 'FG1002');
DELETE FROM ct_line_data WHERE col_b IN ('SMT9001N', 'DIP9001N', 'ASSY9001N', 'TEST9001N');
DELETE FROM product WHERE item_number IN ('FG1001', 'FG1002', 'PCB1001', 'PCB1002', 'ASSY1001', 'ASSY1002');
DELETE FROM family_line WHERE family_code IN ('FAM_DEMO_SMT', 'FAM_DEMO_DIP', 'FAM_DEMO_ASSY', 'FAM_DEMO_TEST');
DELETE FROM product_family WHERE family_code IN ('FAM_DEMO_SMT', 'FAM_DEMO_DIP', 'FAM_DEMO_ASSY', 'FAM_DEMO_TEST');
DELETE FROM manpower_plan WHERE line_class IN ('SMT','DIP','ASS','TES') AND remark = 'CAPICS_DEMO';
DELETE FROM meeting_minutes WHERE mps_version = 'DEMO_V1';
DELETE FROM line_profile WHERE line_code IN ('SMT9001N', 'DIP9001N', 'ASSY9001N', 'TEST9001N');
DELETE FROM line_config WHERE line_code IN ('SMT9001N', 'DIP9001N', 'ASSY9001N', 'TEST9001N');

-- 1) Lines
INSERT INTO line_config(line_code, line_name, working_days_per_week, shifts_per_day, hours_per_shift, is_active, created_by, updated_by)
VALUES
  ('SMT9001N', 'SMT Demo Line', 5, 2, 8.0, true, 'admin', 'admin'),
  ('DIP9001N', 'DIP Demo Line', 5, 2, 8.0, true, 'admin', 'admin'),
  ('ASSY9001N', 'ASSY Demo Line', 5, 2, 8.0, true, 'admin', 'admin'),
  ('TEST9001N', 'TEST Demo Line', 5, 2, 8.0, true, 'admin', 'admin');

INSERT INTO line_profile(line_code, line_class, belong_to, note, updated_by)
VALUES
  ('SMT9001N', 'SMT', 'PCBA', 'CAPICS_DEMO', 'admin'),
  ('DIP9001N', 'DIP', 'PCBA', 'CAPICS_DEMO', 'admin'),
  ('ASSY9001N', 'ASS', 'FA', 'CAPICS_DEMO', 'admin'),
  ('TEST9001N', 'TES', 'FA', 'CAPICS_DEMO', 'admin');

-- 2) Product family + family-line
INSERT INTO product_family(family_code, line_code, coding_rule, cycle_time, oee, worker_count, description, pf, created_by, updated_by)
VALUES
  ('FAM_DEMO_SMT', 'SMT9001N', 'RULE-SMT', 22.50, 88.00, 5, 'SMT Family Demo', 'PF-A', 'admin', 'admin'),
  ('FAM_DEMO_DIP', 'DIP9001N', 'RULE-DIP', 28.00, 86.00, 6, 'DIP Family Demo', 'PF-A', 'admin', 'admin'),
  ('FAM_DEMO_ASSY', 'ASSY9001N', 'RULE-ASSY', 35.00, 84.00, 7, 'ASSY Family Demo', 'PF-B', 'admin', 'admin'),
  ('FAM_DEMO_TEST', 'TEST9001N', 'RULE-TEST', 18.00, 90.00, 4, 'TEST Family Demo', 'PF-B', 'admin', 'admin');

INSERT INTO family_line(family_code, line_code, created_by, updated_by)
VALUES
  ('FAM_DEMO_SMT', 'SMT9001N', 'admin', 'admin'),
  ('FAM_DEMO_DIP', 'DIP9001N', 'admin', 'admin'),
  ('FAM_DEMO_ASSY', 'ASSY9001N', 'admin', 'admin'),
  ('FAM_DEMO_TEST', 'TEST9001N', 'admin', 'admin');

-- 3) Product master
INSERT INTO product(item_number, line_code, family_code, cycle_time, oee, worker_count, description, version, created_by, updated_by)
VALUES
  ('FG1001', 'SMT9001N', 'FAM_DEMO_SMT', 22.50, 88.00, 5, 'Demo Product FG1001', 'DEMO_V1', 'admin', 'admin'),
  ('FG1002', 'SMT9001N', 'FAM_DEMO_SMT', 24.00, 88.00, 5, 'Demo Product FG1002', 'DEMO_V1', 'admin', 'admin'),
  ('PCB1001', 'SMT9001N', 'FAM_DEMO_SMT', 22.50, 88.00, 5, 'Demo PCB1001', 'DEMO_V1', 'admin', 'admin'),
  ('PCB1002', 'DIP9001N', 'FAM_DEMO_DIP', 28.00, 86.00, 6, 'Demo PCB1002', 'DEMO_V1', 'admin', 'admin'),
  ('ASSY1001', 'ASSY9001N', 'FAM_DEMO_ASSY', 35.00, 84.00, 7, 'Demo ASSY1001', 'DEMO_V1', 'admin', 'admin'),
  ('ASSY1002', 'TEST9001N', 'FAM_DEMO_TEST', 18.00, 90.00, 4, 'Demo ASSY1002', 'DEMO_V1', 'admin', 'admin');

-- 4) MRP
INSERT INTO mrp_plan(item_number, description, site, production_line, release_date, due_date, quantity_scheduled, quantity_completed, routing_code, version, created_by, file_name, updated_by)
VALUES
  ('FG1001', 'Demo Product FG1001', '1240', 'SMT9001N', CURRENT_DATE + 1, CURRENT_DATE + 8, 1200, 0, 'R-DEMO-01', 'DEMO_V1', 'admin', 'CAPICS_DEMO_V1.xlsx', 'admin'),
  ('FG1001', 'Demo Product FG1001', '1240', 'SMT9001N', CURRENT_DATE + 9, CURRENT_DATE + 15, 1300, 0, 'R-DEMO-01', 'DEMO_V1', 'admin', 'CAPICS_DEMO_V1.xlsx', 'admin'),
  ('FG1002', 'Demo Product FG1002', '1240', 'SMT9001N', CURRENT_DATE + 2, CURRENT_DATE + 10, 900, 0, 'R-DEMO-02', 'DEMO_V1', 'admin', 'CAPICS_DEMO_V1.xlsx', 'admin');

-- 5) Routing
INSERT INTO routing(product_number, description, version, created_by, updated_by)
VALUES
  ('FG1001', 'Routing Demo FG1001', 'DEMO_V1', 'admin', 'admin'),
  ('FG1002', 'Routing Demo FG1002', 'DEMO_V1', 'admin', 'admin');

INSERT INTO routing_item(routing_id, component_number, line_code, bom_level, bom_quantity)
SELECT id, 'PCB1001', 'SMT9001N', 1, 1 FROM routing WHERE product_number = 'FG1001';
INSERT INTO routing_item(routing_id, component_number, line_code, bom_level, bom_quantity)
SELECT id, 'ASSY1001', 'ASSY9001N', 2, 1 FROM routing WHERE product_number = 'FG1001';
INSERT INTO routing_item(routing_id, component_number, line_code, bom_level, bom_quantity)
SELECT id, 'PCB1002', 'DIP9001N', 1, 1 FROM routing WHERE product_number = 'FG1002';
INSERT INTO routing_item(routing_id, component_number, line_code, bom_level, bom_quantity)
SELECT id, 'ASSY1002', 'TEST9001N', 2, 1 FROM routing WHERE product_number = 'FG1002';

-- 6) Ct-line (主线)
INSERT INTO ct_line_data(col_b, col_c, col_d, col_f, col_i, col_p, col_w, col_x, created_by, updated_by)
VALUES
  ('SMT9001N', 'PCB1001', '主', '22.5', '88', '5', to_char(now(),'YYYY-MM-DD'), 'admin', 'admin', 'admin'),
  ('ASSY9001N', 'ASSY1001', '主', '35', '84', '7', to_char(now(),'YYYY-MM-DD'), 'admin', 'admin', 'admin'),
  ('DIP9001N', 'PCB1002', '主', '28', '86', '6', to_char(now(),'YYYY-MM-DD'), 'admin', 'admin', 'admin'),
  ('TEST9001N', 'ASSY1002', '主', '18', '90', '4', to_char(now(),'YYYY-MM-DD'), 'admin', 'admin', 'admin');

-- 7) Fusion pages data
INSERT INTO manpower_plan(line_class, belong_to, manpower_factor, plan_date, remark, updated_by)
VALUES
  ('SMT', 'PCBA', 1.00, CURRENT_DATE, 'CAPICS_DEMO', 'admin'),
  ('DIP', 'PCBA', 0.95, CURRENT_DATE, 'CAPICS_DEMO', 'admin'),
  ('ASS', 'FA', 0.90, CURRENT_DATE, 'CAPICS_DEMO', 'admin'),
  ('TES', 'FA', 1.05, CURRENT_DATE, 'CAPICS_DEMO', 'admin');

INSERT INTO meeting_minutes(mps_version, item_no, minutes, remark, updated_by)
VALUES
  ('DEMO_V1', 1, 'Demo meeting minutes for CAPICS smoke test.', 'CAPICS_DEMO', 'admin');

COMMIT;
