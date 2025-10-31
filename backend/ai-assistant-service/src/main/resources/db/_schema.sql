CREATE TABLE IF NOT EXISTS chat_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    session_name VARCHAR(255) NOT NULL,
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id UUID NOT NULL,
    sender VARCHAR(50) NOT NULL,
    content TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ai_interaction_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id UUID NOT NULL,
    interaction_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP(6) NOT NULL,
    model_name VARCHAR(100),
    latency_ms BIGINT,
    prompt_tokens INT,
    response_tokens INT,
    details TEXT, -- Using TEXT for JSON string
    FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
);

