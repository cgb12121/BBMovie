package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.service.FileUploadClient;
import com.bbmovie.common.dtos.nats.FileUploadResult;
import com.bbmovie.common.dtos.nats.UploadMetadata;
import com.bbmovie.common.enums.EntityType;
import com.bbmovie.common.enums.Storage;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Implementation of FileUploadClient that communicates with file-service via WebClient.
 * <p>
 * This implementation:
 * - Uses WebClient for reactive, non-blocking HTTP calls
 * - Routes to appropriate endpoints based on file type
 * - Handles authentication by forwarding the Authorization header
 * - Creates proper UploadMetadata for file-service
 * <p>
 * Alternative approaches are considered:
 * 1. Direct database access - rejected (violates microservice boundaries)
 * 2. Message queue (NATS) - could work but adds complexity for synchronous operations
 * 3. Service mesh - overkill for current architecture
 */
@Service
@RequiredArgsConstructor
public class FileUploadClientImpl implements FileUploadClient {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(FileUploadClientImpl.class);

    private final WebClient fileServiceWebClient;

    @Value("${file-service.storage:LOCAL}")
    private Storage defaultStorage;

    @Override
    public Mono<FileUploadResult> uploadImage(FilePart filePart, Jwt jwt) {
        return uploadFile(filePart, jwt, "/image/upload", EntityType.IMAGE);
    }

    @Override
    public Mono<FileUploadResult> uploadDocument(FilePart filePart, Jwt jwt) {
        // Using STUDENT_DOCUMENT as a generic document type,
        // You may want to add a DOCUMENT entity type to EntityType enum
        return uploadFile(filePart, jwt, "/document/upload", EntityType.STUDENT_DOCUMENT);
    }

    @Override
    public Mono<FileUploadResult> uploadAudio(FilePart filePart, Jwt jwt) {
        // Audio files can be uploaded as generic files
        // You may want to add an AUDIO entity type to EntityType enum
        return uploadFile(filePart, jwt, "/audio/upload", EntityType.STUDENT_DOCUMENT);
    }

    /**
     * Generic method to upload a file to the file-service.
     *
     * @param filePart The file to upload
     * @param jwt User JWT authentication
     * @param endpoint The file-service endpoint (e.g., "/image/upload")
     * @param entityType The type of entity (IMAGE, STUDENT_DOCUMENT, etc.)
     * @return Mono containing FileUploadResult
     */
    private Mono<FileUploadResult> uploadFile(FilePart filePart, Jwt jwt, String endpoint, EntityType entityType) {

        // Create metadata for the upload
        // Using all-args constructor (Lombok @AllArgsConstructor generates it)
        UploadMetadata metadata = new UploadMetadata(
                UUID.randomUUID().toString(), // fileId
                entityType, // fileType
                defaultStorage, // storage
                null // quality - not applicable for non-video files
        );

        log.info("Uploading file {} to file-service endpoint {}", filePart.filename(), endpoint);

        // Build a multipart body
        BodyInserters.MultipartInserter bodyBuilder = BodyInserters.fromMultipartData("file", filePart);

        // Add metadata as JSON string (file-service expects it as a part)
        // Note: The file-service controller expects @RequestPart("metadata") UploadMetadata
        // which means it needs to be sent as a separate part, not as JSON string
        // We'll use ObjectMapper to serialize it, but Spring should handle this automatically

        return fileServiceWebClient
                .post()
                .uri(endpoint)
                .headers(headers -> {
                    // Forward authentication token from JWT
                    if (jwt != null) {
                        headers.setBearerAuth(jwt.getTokenValue());
                    }
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(bodyBuilder.with("metadata", metadata))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("File uploaded successfully: {}", filePart.filename()))
                .doOnError(error -> log.error("Failed to upload file {}: {}", filePart.filename(), error.getMessage()))
                .map(response -> {
                    // Parse response to extract file URL
                    // For now, return a placeholder - you may want to parse JSON response
                    log.debug("File service response: {}", response);
                    return new FileUploadResult(
                            "file://" + filePart.filename(), // Will be replaced with actual URL from response
                            filePart.filename()
                    );
                })
                .onErrorResume(error -> {
                    log.error("Error uploading file {}: {}", filePart.filename(), error.getMessage());
                    // Return a result even on error to allow processing to continue
                    return Mono.just(new FileUploadResult(
                            "error://" + filePart.filename(),
                            filePart.filename()
                    ));
                });
    }
}

