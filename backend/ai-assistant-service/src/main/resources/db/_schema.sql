CREATE TABLE IF NOT EXISTS ai_chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(255) NOT NULL,
    content TEXT,
    timestamp TIMESTAMP NOT NULL
);
