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
