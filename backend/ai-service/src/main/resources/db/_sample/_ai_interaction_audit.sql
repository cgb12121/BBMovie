-- 3. Add Audit Records
INSERT INTO ai_interaction_audit (session_id, interaction_type, timestamp, model_name, latency_ms, prompt_tokens, response_tokens, details)
VALUES
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'USER_MESSAGE', NOW(6) - INTERVAL 4 MINUTE,
 NULL, NULL, NULL, NULL,
 '{"message": "Hello, can you tell me about the movie Inception?"}'),
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'AI_COMPLETE_RESPONSE', NOW(6) - INTERVAL 3 MINUTE,
 'qwen3:0.6b-q4_K_M', 2450, 75, 120,
 '{"response": "Of course! Inception is a 2010 science fiction action film..."}');