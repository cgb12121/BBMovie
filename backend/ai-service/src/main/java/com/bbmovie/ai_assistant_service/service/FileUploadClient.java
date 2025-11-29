package com.bbmovie.ai_assistant_service.service;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.oauth2.jwt.Jwt;

import com.bbmovie.common.dtos.nats.FileUploadResult;

import reactor.core.publisher.Mono;

/**
 * Client service for uploading files to the file-service microservice.
 * <p>
 * This service handles the communication with the file-service to persist
 * files (images, PDFs, audio) that are uploaded as part of chat requests.
 * <p>
 * Flow:
 * 1. Receives a FilePart from the chat request
 * 2. Determines the appropriate endpoint based on file type (image, document, audio)
 * 3. Creates UploadMetadata with proper EntityType and Storage
 * 4. Calls the file-service to persist the file
 * 5. Returns FileUploadResult with file URL and metadata
 * <p>
 * Why separate service:
 * - Encapsulates file-service communication logic
 * - Makes it easy to switch between direct calls or service discovery
 * - Can be easily mocked for testing
 */
public interface FileUploadClient {
    /**
     * Uploads an image file to the file-service.
     *
     * @param filePart The image file to upload
     * @param jwt User JWT authentication for authorization
     * @return Mono containing FileUploadResult with file URL and metadata
     */
    Mono<FileUploadResult> uploadImage(FilePart filePart, Jwt jwt);

    /**
     * Uploads a document file (PDF, text) to the file-service.
     *
     * @param filePart The document file to upload
     * @param jwt User JWT authentication for authorization
     * @return Mono containing FileUploadResult with file URL and metadata
     */
    Mono<FileUploadResult> uploadDocument(FilePart filePart, Jwt jwt);

    /**
     * Uploads an audio file to the file-service.
     *
     * @param filePart The audio file to upload
     * @param jwt User JWT authentication for authorization
     * @return Mono containing FileUploadResult with file URL and metadata
     */
    Mono<FileUploadResult> uploadAudio(FilePart filePart, Jwt jwt);
}

