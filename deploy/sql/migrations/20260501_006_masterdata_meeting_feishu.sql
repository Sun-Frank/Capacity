-- CAPICS v2026.05.01 master-data, meeting, and Feishu incremental migration.

CREATE TABLE IF NOT EXISTS project_master (
    id BIGSERIAL PRIMARY KEY,
    customer VARCHAR(100),
    product_platform VARCHAR(100),
    vehicle_config VARCHAR(100),
    product_description VARCHAR(255) NOT NULL,
    bws VARCHAR(100),
    version VARCHAR(50),
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_project_master_description
    ON project_master (LOWER(product_description));

ALTER TABLE routing_item
    ALTER COLUMN line_code DROP NOT NULL;

ALTER TABLE ct_line_data
    ADD COLUMN IF NOT EXISTS finished_item_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS product_description VARCHAR(255);

ALTER TABLE line_config
    ADD COLUMN IF NOT EXISTS process_segment VARCHAR(100);

CREATE TABLE IF NOT EXISTS notebook_note (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS feishu_config (
    id INTEGER PRIMARY KEY,
    api_url VARCHAR(255),
    app_id VARCHAR(120),
    app_secret TEXT,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE meeting_minutes
    ADD COLUMN IF NOT EXISTS product_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS product_description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS line_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS adjustment_field VARCHAR(100),
    ADD COLUMN IF NOT EXISTS before_value VARCHAR(255),
    ADD COLUMN IF NOT EXISTS after_value VARCHAR(255),
    ADD COLUMN IF NOT EXISTS owner_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'OPEN';

ALTER TABLE product
    ALTER COLUMN line_code SET DEFAULT 'MASTER';
