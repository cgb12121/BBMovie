package com.bbmovie.mediauploadservice.controller;

import com.bbmovie.mediauploadservice.dto.*;
import com.bbmovie.mediauploadservice.service.ChunkedUploadService;
import com.bbmovie.mediauploadservice.service.ClientUploadService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final ClientUploadService clientUploadService;
    private final ChunkedUploadService chunkedUploadService;
    private final com.bbmovie.mediauploadservice.service.StsCredentialsService stsCredentialsService;

    //presign for the client to upload
    @PostMapping("/init")
    public ResponseEntity<@NonNull UploadInitResponse> initUpload(
            @RequestBody @Valid UploadInitRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(clientUploadService.initUpload(request, jwt));
    }

    @GetMapping("/files/{uploadId}/url")
    public ResponseEntity<@NonNull Map<String, String>> getDownloadUrl(
            @PathVariable String uploadId,
            @AuthenticationPrincipal Jwt jwt) {
        String url = clientUploadService.generateDownloadUrl(uploadId, jwt);
        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }

    // Chunked upload endpoints
    @PostMapping("/chunked/init")
    public ResponseEntity<@NonNull ChunkedUploadInitResponse> initChunkedUpload(
            @RequestBody @Valid ChunkedUploadInitRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(clientUploadService.initChunkedUpload(request, jwt));
    }

    @PostMapping("/chunked/complete")
    public ResponseEntity<@NonNull Map<String, String>> completeChunkedUpload(
            @RequestBody @Valid CompleteChunkedUploadRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        clientUploadService.completeChunkedUpload(request, jwt);
        return ResponseEntity.ok(Map.of("status", "completed", "uploadId", request.getUploadId()));
    }

    // Chunk management endpoints

    /**
     * Get batch of chunk URLs (lazy generation)
     * GET /upload/{uploadId}/chunks?from=1&to=10
     */
    @GetMapping("/{uploadId}/chunks")
    public ResponseEntity<@NonNull ChunkBatchResponse> getChunkBatch(
            @PathVariable String uploadId,
            @RequestParam Integer from,
            @RequestParam Integer to,
            @AuthenticationPrincipal Jwt jwt) {
        ChunkBatchResponse response = chunkedUploadService.getChunkBatchUrls(uploadId, from, to, jwt);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a chunk as uploaded with ETag
     * POST /upload/{uploadId}/chunks/{partNumber}/complete
     */
    @PostMapping("/{uploadId}/chunks/{partNumber}/complete")
    public ResponseEntity<@NonNull Map<String, String>> markChunkComplete(
            @PathVariable String uploadId,
            @PathVariable Integer partNumber,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {
        String etag = request.get("etag");
        if (etag == null || etag.trim().isEmpty()) {
            throw new IllegalArgumentException("ETag is required");
        }
        chunkedUploadService.markChunkUploaded(uploadId, partNumber, etag, jwt);
        return ResponseEntity.ok(Map.of(
            "status", "completed",
            "uploadId", uploadId,
            "partNumber", String.valueOf(partNumber)
        ));
    }

    /**
     * Retry a failed chunk
     * POST /upload/{uploadId}/chunks/{partNumber}/retry
     */
    @PostMapping("/{uploadId}/chunks/{partNumber}/retry")
    public ResponseEntity<@NonNull ChunkUploadInfo> retryChunk(
            @PathVariable String uploadId,
            @PathVariable Integer partNumber,
            @AuthenticationPrincipal Jwt jwt) {
        ChunkUploadInfo chunkInfo = chunkedUploadService.retryChunk(uploadId, partNumber, jwt);
        return ResponseEntity.ok(chunkInfo);
    }

    /**
     * Get upload progress
     * GET /upload/{uploadId}/chunks/status
     */
    @GetMapping("/{uploadId}/chunks/status")
    public ResponseEntity<@NonNull ChunkUploadProgressResponse> getUploadProgress(
            @PathVariable String uploadId,
            @AuthenticationPrincipal Jwt jwt) {
        ChunkUploadProgressResponse response = chunkedUploadService.getUploadProgress(uploadId, jwt);
        return ResponseEntity.ok(response);
    }

    // STS Credentials endpoint
    @PostMapping("/sts/credentials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<@NonNull StsCredentialsResponse> getStsCredentials(
            @RequestBody @Valid StsCredentialsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(stsCredentialsService.generateTemporaryCredentials(request, jwt));
    }
}
