-- CAPICS Database Schema
-- PostgreSQL

-- Drop tables if exist (in correct order due to foreign keys)
DROP TABLE IF EXISTS line_realtime CASCADE;
DROP TABLE IF EXISTS routing_item CASCADE;
DROP TABLE IF EXISTS routing CASCADE;
DROP TABLE IF EXISTS line_config CASCADE;
DROP TABLE IF EXISTS mrp_plan CASCADE;
DROP TABLE IF EXISTS product CASCADE;
DROP TABLE IF EXISTS product_family CASCADE;
DROP TABLE IF EXISTS family_line CASCADE;
DROP TABLE IF EXISTS sys_user_role CASCADE;
DROP TABLE IF EXISTS sys_role CASCADE;
DROP TABLE IF EXISTS sys_user CASCADE;

-- System User Tables
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    email VARCHAR(100),
    enabled BOOLEAN DEFAULT true,
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) UNIQUE NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    UNIQUE(user_id, role_id)
);

-- Product Family Table (编码族)
CREATE TABLE product_family (
    family_code VARCHAR(50) NOT NULL,
    line_code VARCHAR(50) NOT NULL,
    coding_rule VARCHAR(100),
    cycle_time DECIMAL(10,2),
    oee DECIMAL(5,2),
    worker_count INTEGER,
    version VARCHAR(20),
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (family_code, line_code)
);

-- Family Line Table (编码族定线)
CREATE TABLE family_line (
    family_code VARCHAR(50) NOT NULL,
    line_code VARCHAR(50) NOT NULL,
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (family_code, line_code)
);

-- Product Master Data Table (产品主数据)
CREATE TABLE product (
    item_number VARCHAR(50) NOT NULL,
    line_code VARCHAR(50) NOT NULL,
    family_code VARCHAR(50),
    cycle_time DECIMAL(10,2),
    oee DECIMAL(5,2),
    worker_count INTEGER,
    description VARCHAR(255),
    version VARCHAR(20),
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (item_number, line_code),
    FOREIGN KEY (family_code, line_code) REFERENCES product_family(family_code, line_code)
);

-- MRP Plan Table
CREATE TABLE mrp_plan (
    id BIGSERIAL PRIMARY KEY,
    item_number VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    site VARCHAR(50) NOT NULL,
    production_line VARCHAR(100),
    release_date DATE NOT NULL,
    due_date DATE,
    quantity_scheduled DECIMAL(18,2) NOT NULL,
    quantity_completed DECIMAL(18,2) DEFAULT 0,
    routing_code VARCHAR(50),
    version VARCHAR(10) NOT NULL,
    created_by VARCHAR(50),
    file_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Routing Table (工艺路线)
CREATE TABLE routing (
    id BIGSERIAL PRIMARY KEY,
    product_number VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    version VARCHAR(20),
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Routing Item Table (BOM明细)
CREATE TABLE routing_item (
    id BIGSERIAL PRIMARY KEY,
    routing_id BIGINT NOT NULL REFERENCES routing(id) ON DELETE CASCADE,
    component_number VARCHAR(50) NOT NULL,
    line_code VARCHAR(50) NOT NULL,
    bom_level INTEGER,
    bom_quantity DECIMAL(10,2) DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Line Config Table (生产线配置)
CREATE TABLE line_config (
    line_code VARCHAR(50) PRIMARY KEY,
    line_name VARCHAR(100),
    working_days_per_week INTEGER DEFAULT 5,
    shifts_per_day INTEGER DEFAULT 2,
    hours_per_shift DECIMAL(4,1) DEFAULT 8.0,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Line Realtime Table (生产线实时配置)
CREATE TABLE line_realtime (
    id BIGSERIAL PRIMARY KEY,
    line_code VARCHAR(50) NOT NULL,
    item_number VARCHAR(50) NOT NULL,
    component_number VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    shift_output DECIMAL(12,2),
    shift_workers INTEGER,
    ct DECIMAL(10,2),
    oee DECIMAL(5,4),
    weekly_demand VARCHAR(1000),
    mrp_version VARCHAR(10),
    calculated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (line_code, item_number, component_number, mrp_version)
);

-- Create indexes
CREATE INDEX idx_mrp_plan_version ON mrp_plan(version);
CREATE INDEX idx_mrp_plan_release_date ON mrp_plan(release_date);
CREATE INDEX idx_mrp_plan_item ON mrp_plan(item_number);
CREATE INDEX idx_mrp_plan_created_by ON mrp_plan(created_by);
CREATE INDEX idx_mrp_plan_file_name ON mrp_plan(file_name);
CREATE INDEX idx_mrp_plan_cbf ON mrp_plan(created_by, file_name);
CREATE INDEX idx_routing_product ON routing(product_number);
CREATE INDEX idx_routing_item_routing ON routing_item(routing_id);
CREATE INDEX idx_product_family_code ON product_family(family_code);
CREATE INDEX idx_product_item ON product(item_number);

-- Insert default admin user (password: admin123)
INSERT INTO sys_user (username, password, real_name, email, enabled)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'System Admin', 'admin@capics.com', true);

INSERT INTO sys_role (role_code, role_name, description)
VALUES ('ADMIN', 'Administrator', 'System administrator with full access');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'admin' AND r.role_code = 'ADMIN';

-- Insert default line configs
INSERT INTO line_config (line_code, line_name, working_days_per_week, shifts_per_day, hours_per_shift)
VALUES
    ('SMT1001N', 'SMT Line 1', 5, 2, 8.0),
    ('SMT1002N', 'SMT Line 2', 5, 2, 8.0),
    ('DIP1001N', 'DIP Line 1', 5, 2, 8.0),
    ('ASSY1001N', 'Assembly Line 1', 5, 2, 8.0),
    ('TEST1001N', 'Test Line 1', 5, 2, 8.0);
