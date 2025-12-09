CREATE TABLE IF NOT EXISTS file_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id VARCHAR(255),
    entity_type VARCHAR(50),
    storage_provider VARCHAR(50),
    path_or_public_id VARCHAR(500),
    quality VARCHAR(50),
    mime_type VARCHAR(100),
    file_size BIGINT,
    status VARCHAR(50) DEFAULT 'CONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS temp_file_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id VARCHAR(255),
    file_type VARCHAR(50),
    storage VARCHAR(50),
    temp_file_path VARCHAR(500),
    username VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
