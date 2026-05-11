CREATE TABLE IF NOT EXISTS wms_bom_config (
    id INTEGER PRIMARY KEY,
    login_url VARCHAR(255),
    username VARCHAR(120),
    password_value TEXT,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW()
);
