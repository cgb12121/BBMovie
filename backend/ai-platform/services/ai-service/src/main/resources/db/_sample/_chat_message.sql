-- 2. Add Chat Messages
INSERT INTO chat_message (session_id, sender, content, timestamp)
VALUES
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'USER',
 'Hello, can you tell me about the movie Inception?', NOW(6) - INTERVAL 4 MINUTE),
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'AI',
 'Of course! Inception is a 2010 science fiction action film written and directed by Christopher Nolan.', NOW(6) - INTERVAL 3 MINUTE);

