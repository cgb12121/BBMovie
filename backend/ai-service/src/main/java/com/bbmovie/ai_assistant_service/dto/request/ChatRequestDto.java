package com.bbmovie.ai_assistant_service.dto.request;

import java.util.ArrayList;
import java.util.List;

import com.bbmovie.ai_assistant_service.entity.model.AiMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for chat requests that can include text message and file references.
 * 
 * Files are uploaded separately via multipart/form-data and processed
 * before being included in this DTO as file references (URLs).
 * 
 * Why this design:
 * - Separates file upload (multipart) from chat request (JSON)
 * - File references are persisted URLs, not raw file data
 * - Makes it easy to support both file uploads and text-only messages
 */
@Data
public class ChatRequestDto {
    @NotBlank
    private String message;
    @NotBlank
    private String assistantType; // e.g., "admin", "user"
    @NotNull
    private AiMode aiMode;
    
    /**
     * List of file references (URLs) that were uploaded and processed.
     * These files are already persisted in the file-service.
     */
    private List<String> fileReferences = new ArrayList<>();
    
    /**
     * Extracted text content from files (e.g., audio transcriptions, PDF text).
     * This text is automatically included in the message context.
     */
    private String extractedFileContent;
}
