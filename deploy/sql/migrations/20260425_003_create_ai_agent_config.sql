CREATE TABLE IF NOT EXISTS ai_agent_config (
    id INTEGER PRIMARY KEY,
    api_key TEXT,
    base_url VARCHAR(255),
    model VARCHAR(120),
    timeout_ms INTEGER,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW()
);
