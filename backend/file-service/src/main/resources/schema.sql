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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS outbox_file_record (
    id VARCHAR(255) PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    extension VARCHAR(20),
    size BIGINT NOT NULL,
    temp_dir VARCHAR(500),
    temp_store_for VARCHAR(255),
    uploaded_by VARCHAR(255),
    is_removed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(255) PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL
);
