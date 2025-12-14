CREATE TABLE IF NOT EXISTS approval_requests (
    id VARCHAR(36) PRIMARY KEY,
    approval_token VARCHAR(36) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    message_id VARCHAR(255),
    action_type VARCHAR(50) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    tool_name VARCHAR(255),
    payload TEXT,
    request_status VARCHAR(20) NOT NULL, -- Renamed from status to avoid reserved word issues
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    approved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approval_token_session ON approval_requests(approval_token, session_id);
