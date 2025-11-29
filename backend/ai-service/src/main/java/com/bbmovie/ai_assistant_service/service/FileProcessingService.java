package com.bbmovie.ai_assistant_service.service;

import java.util.List;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.oauth2.jwt.Jwt;

import com.bbmovie.common.dtos.nats.FileUploadResult;

import reactor.core.publisher.Mono;

/**
 * Service for processing files uploaded as part of chat requests.
 * <p>
 * This service orchestrates:
 * 1. File upload to file-service (persistence)
 * 2. Audio transcription (for audio files using WhisperService)
 * 3. File content extraction (for PDFs, text files - future)
 * <p>
 * Flow for different file types:
 * - Images: Upload → Return file reference for AI context
 * - PDFs: Upload → Extract text (future) → Return text + file reference
 * - Text files: Upload → Extract text → Return text + file reference
 * - Audio: Upload → Transcribe → Return transcription + file reference
 * <p>
 * Why separate service:
 * - Single responsibility: handles all file processing logic
 * - Easy to extend with new file types
 * - Can be tested independently
 */
public interface FileProcessingService {
    /**
     * Processes a list of files uploaded with a chat request.
     *
     * @param files List of FilePart objects to process
     * @param jwt   User JWT authentication
     * @return Mono containing FileProcessingResult with processed file information
     */
    Mono<FileProcessingResult> processFiles(List<FilePart> files, Jwt jwt);

    /**
     * Result of file processing containing:
     * - File references (URLs) for all uploaded files
     * - Processed file content (extracted text with metadata)
     * - File metadata (type, size, etc.)
     *
     * @param uploadedFiles  List of all uploaded files results
     * @param processedFiles List of files that were processed
     *                       (transcribed/extracted)
     * @param fileReferences URLs or paths to uploaded files (convenience list)
     */
    record FileProcessingResult(
            List<FileUploadResult> uploadedFiles,
            List<ProcessedFileContent> processedFiles,
            List<String> fileReferences) {
    }

    /**
     * Represents the content extracted from a processed file.
     * 
     * @param filename      Original filename
     * @param fileType      Type of file (audio, pdf, etc.)
     * @param url           Public URL or reference to the file
     * @param extractedText Text extracted from the file (transcription, OCR, etc.)
     */
    record ProcessedFileContent(
            String filename,
            String fileType,
            String url,
            String extractedText) {
    }
}
