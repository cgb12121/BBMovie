package com.bbmovie.ai_assistant_service.service;

import java.util.List;


import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto.FileAttachment;
import com.bbmovie.common.dtos.nats.FileUploadResult;

import reactor.core.publisher.Mono;

/**
 * Service for processing files uploaded as part of chat requests.
 */
public interface FileProcessingService {

    /**
     * Processes a list of file attachments (already uploaded by client).
     *
     * @param attachments List of FileAttachment DTOs
     * @return Mono containing FileProcessingResult with processed file information
     */
    Mono<FileProcessingResult> processAttachments(List<FileAttachment> attachments);

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