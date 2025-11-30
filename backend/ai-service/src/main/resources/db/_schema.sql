CREATE TABLE IF NOT EXISTS chat_session (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    session_name VARCHAR(255) NOT NULL,
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    sender VARCHAR(50) NOT NULL,
    content TEXT,
    timestamp TIMESTAMP(6) NOT NULL,
    file_content_json JSON NULL COMMENT 'JSON string containing file references, extracted content, and content type',
    FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ai_interaction_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    interaction_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP(6) NOT NULL,
    model_name VARCHAR(100),
    latency_ms BIGINT,
    prompt_tokens INT,
    response_tokens INT,
    details TEXT,
    FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
);
