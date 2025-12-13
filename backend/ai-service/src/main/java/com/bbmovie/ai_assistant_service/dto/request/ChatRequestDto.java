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
    
    @Builder.Default
    private List<String> fileReferences = new ArrayList<>();
    
    private String extractedFileContent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileAttachment {
        private Long fileId;
        private String fileUrl; 
        private String storageType;
        private String filename;
    }
}