CREATE TABLE IF NOT EXISTS ct_line_data (
    id BIGSERIAL PRIMARY KEY,
    col_b VARCHAR(255) NOT NULL,
    col_c VARCHAR(255) NOT NULL,
    col_d VARCHAR(255) NOT NULL,
    col_f VARCHAR(255) NOT NULL,
    col_i VARCHAR(255) NOT NULL,
    col_p VARCHAR(255) NOT NULL,
    col_w VARCHAR(255),
    col_x VARCHAR(255),
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ct_line_data_created_at ON ct_line_data(created_at);
