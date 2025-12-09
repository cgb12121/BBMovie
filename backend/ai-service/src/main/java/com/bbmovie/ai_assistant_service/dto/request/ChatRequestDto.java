package com.bbmovie.ai_assistant_service.dto.request;

import java.util.ArrayList;
import java.util.List;

import com.bbmovie.ai_assistant_service.entity.model.AiMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {
    @NotBlank
    private String message;
    @NotBlank
    private String assistantType; 
    @NotNull
    private AiMode aiMode;
    
    @Builder.Default
    private List<FileAttachment> attachments = new ArrayList<>();
    
    /**
     * List of file references (URLs) for the AI context.
     * Populated by ChatService after processing attachments.
     */
    @Builder.Default
    private List<String> fileReferences = new ArrayList<>();
    
    /**
     * Extracted text content from files.
     * Populated by ChatService after processing attachments.
     */
    private String extractedFileContent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileAttachment {
        private Long fileId;
        private String fileUrl; // Download URL or Internal Path
        private String storageType;
        private String filename;
    }
}
