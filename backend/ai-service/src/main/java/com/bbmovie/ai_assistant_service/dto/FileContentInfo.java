package com.bbmovie.ai_assistant_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileContentInfo {
    
    @JsonProperty("file_references")
    private List<String> fileReferences;
    
    @JsonProperty("extracted_content")
    private String extractedContent;
    
    @JsonProperty("file_content_type")
    private String fileContentType; // Type of file content (IMAGE_URL, AUDIO_TRANSCRIPT, DOCUMENT_TEXT, etc.)
}