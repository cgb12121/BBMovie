-- Migration: Add file_content_json column to chat_message table
-- This migration adds a column to store file content as JSON string for messages with attached files

-- Add file_content_json column to chat_message table
ALTER TABLE chat_message 
ADD COLUMN file_content_json JSON NULL COMMENT 'JSON string containing file references, extracted content, and content type';

-- Update existing data if needed (for any existing messages with file info, if there were any in a different format)
-- In this case, there shouldn't be any since we're adding the new functionality
-- The column is nullable and will remain null for existing messages without file content

-- Example of how to insert a message with file content (for testing purposes):
-- INSERT INTO chat_message (session_id, sender, content, timestamp, file_content_json)
-- VALUES 
-- ('2f893bb5-1ca0-473d-99b8-6d0a09d815a6', 'USER',
--  'Can you help me analyze this audio file?', NOW(6),
--  '{"file_references":["https://example.com/audio1.mp3"],"extracted_content":"This is the transcribed content of the audio file...","file_content_type":"AUDIO_TRANSCRIPT"}');
