CREATE TABLE IF NOT EXISTS file_assets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id VARCHAR(255),
    entity_type VARCHAR(255),
    storage_provider VARCHAR(255),
    path_or_public_id VARCHAR(255),
    quality VARCHAR(255),
    mime_type VARCHAR(255),
    file_size BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(255) PRIMARY KEY,
    aggregate_type VARCHAR(255),
    aggregate_id VARCHAR(255),
    event_type VARCHAR(255),
    payload TEXT,
    status VARCHAR(255),
    retry_count INT,
    created_at TIMESTAMP,
    last_attempt_at TIMESTAMP,
    sent_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS outbox_file_record (
    id VARCHAR(255) PRIMARY KEY,
    file_name VARCHAR(255),
    extension VARCHAR(255),
    size BIGINT,
    temp_dir VARCHAR(255),
    temp_store_for VARCHAR(255),
    uploaded_by VARCHAR(255),
    is_removed BOOLEAN,
    created_at TIMESTAMP,
    removed_at TIMESTAMP
);
