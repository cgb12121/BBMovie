package com.bbmovie.ai_assistant_service.service;

import java.util.List;

import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto.FileAttachment;
import com.bbmovie.common.dtos.nats.FileUploadResult;
import org.springframework.security.oauth2.jwt.Jwt;

import reactor.core.publisher.Mono;

/**
 * Service for processing files uploaded as part of chat requests.
 */
public interface FileProcessingService {

    /**
     * Processes a list of file attachments (already uploaded by client).
     * This method will attempt to get file URLs from media-upload-service if needed.
     *
     * @param attachments List of FileAttachment DTOs
     * @param jwt JWT token for authenticating with media-upload-service (required if uploadId is provided)
     * @return Mono containing FileProcessingResult with processed file information
     */
    Mono<FileProcessingResult> processAttachments(List<FileAttachment> attachments, Jwt jwt);

    record FileProcessingResult(
            List<FileUploadResult> uploadedFiles, // Can be empty if using attachments
            List<ProcessedFileContent> processedFiles,
            List<String> fileReferences) {
        
        public static FileProcessingResult empty() {
            return new FileProcessingResult(List.of(), List.of(), List.of());
        }
    }

    record ProcessedFileContent(
            String filename,
            String fileType,
            String url,
            String extractedText) {
    }
}