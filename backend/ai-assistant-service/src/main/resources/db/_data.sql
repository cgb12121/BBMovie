-- 1. Create a Chat Session
INSERT IGNORE INTO chat_session (id, user_id, session_name, is_archived, created_at, updated_at)
VALUES (
    '2f893bb5-1ca0-473d-99b8-6d0a09d815a6',
    '8a8b3c51-7fdb-409c-a3d9-5458b2d05e31',
    'Dummy Chat About Movies',
    FALSE,
    NOW(6) - INTERVAL 10 MINUTE,
    NOW(6) - INTERVAL 5 MINUTE
);

-- 2. Add Chat Messages
INSERT INTO chat_message (session_id, sender, content, timestamp)
VALUES
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'USER',
 'Hello, can you tell me about the movie Inception?', NOW(6) - INTERVAL 4 MINUTE),
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'AI',
 'Of course! Inception is a 2010 science fiction action film written and directed by Christopher Nolan.', NOW(6) - INTERVAL 3 MINUTE);

-- 3. Add Audit Records
INSERT INTO ai_interaction_audit (session_id, interaction_type, timestamp, model_name, latency_ms, prompt_tokens, response_tokens, details)
VALUES
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'USER_MESSAGE', NOW(6) - INTERVAL 4 MINUTE,
 NULL, NULL, NULL, NULL,
 '{"message": "Hello, can you tell me about the movie Inception?"}'),
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'AI_COMPLETE_RESPONSE', NOW(6) - INTERVAL 3 MINUTE,
 'qwen3:0.6b-q4_K_M', 2450, 75, 120,
 '{"response": "Of course! Inception is a 2010 science fiction action film..."}');
