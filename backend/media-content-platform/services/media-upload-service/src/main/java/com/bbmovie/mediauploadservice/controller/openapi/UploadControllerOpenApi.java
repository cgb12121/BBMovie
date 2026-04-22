package com.bbmovie.mediauploadservice.controller.openapi;

import com.bbmovie.mediauploadservice.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Tag(name = "Upload", description = "Media upload and chunk upload APIs")
public interface UploadControllerOpenApi {
    @Operation(summary = "Init upload", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull UploadInitResponse> initUpload(@RequestBody @Valid UploadInitRequest request, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Get download URL", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull Map<String, String>> getDownloadUrl(@PathVariable String uploadId, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Init chunked upload", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull ChunkedUploadInitResponse> initChunkedUpload(@RequestBody @Valid ChunkedUploadInitRequest request, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Complete chunked upload", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull Map<String, String>> completeChunkedUpload(@RequestBody @Valid CompleteChunkedUploadRequest request, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Get chunk URL batch", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull ChunkBatchResponse> getChunkBatch(@PathVariable String uploadId, @RequestParam Integer from, @RequestParam Integer to, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Mark chunk complete", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull Map<String, String>> markChunkComplete(@PathVariable String uploadId, @PathVariable Integer partNumber, @RequestBody Map<String, String> request, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Retry chunk", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull ChunkUploadInfo> retryChunk(@PathVariable String uploadId, @PathVariable Integer partNumber, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Get upload progress", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull ChunkUploadProgressResponse> getUploadProgress(@PathVariable String uploadId, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Get STS credentials (admin)", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull StsCredentialsResponse> getStsCredentials(@RequestBody @Valid StsCredentialsRequest request, @AuthenticationPrincipal Jwt jwt);
}

