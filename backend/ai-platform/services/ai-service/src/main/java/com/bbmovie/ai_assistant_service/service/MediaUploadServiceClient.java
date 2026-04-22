package com.bbmovie.ai_assistant_service.service;

import reactor.core.publisher.Mono;

/**
 * Client interface for communicating with media-upload-service.
 * Used to retrieve file download URLs by uploadId.
 */
public interface MediaUploadServiceClient {
    /**
     * Gets a download URL for a file by its uploadId.
     * The URL is a presigned URL that allows temporary access to the file.
     * 
     * @param uploadId The upload ID (UUID string) from media-upload-service
     * @param jwtToken The JWT token for authentication
     * @return Mono containing the download URL
     */
    Mono<String> getDownloadUrl(String uploadId, String jwtToken);
}

