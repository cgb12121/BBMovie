-- Sample: Insert chat messages with file content
-- This file demonstrates how to insert messages with attached file content in JSON format

-- Insert a user message with attached audio file (transcribed)
INSERT INTO chat_message (session_id, sender, content, timestamp, file_content_json)
VALUES
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'USER',
 'Can you analyze this audio?', NOW(6) - INTERVAL 2 MINUTE,
 '{"file_references":["https://storage.example.com/audio/recording_001.mp3"],"extracted_content":"Hello, this is a sample audio file. Please transcribe this for me.","file_content_type":"AUDIO_TRANSCRIPT"}'),

-- Insert an AI response to that message
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'AI',
 'I''ve analyzed the audio file. The transcription is: "Hello, this is a sample audio file. Please transcribe this for me."', NOW(6) - INTERVAL 1 MINUTE,
 NULL),

-- Insert a user message with attached image
('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'USER',
 'What do you see in this image?', NOW(6),
 '{"file_references":["https://storage.example.com/images/photo_001.jpg"],"extracted_content":"","file_content_type":"IMAGE_URL"}');