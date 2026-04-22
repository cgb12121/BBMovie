package com.bbmovie.mediauploadservice.controller.openapi;

import com.bbmovie.mediauploadservice.dto.MediaFileFilterRequest;
import com.bbmovie.mediauploadservice.dto.MediaFileResponse;
import com.bbmovie.mediauploadservice.entity.MediaFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;

@SuppressWarnings("unused")
@Tag(name = "Media Management", description = "Admin and user APIs for uploaded media files")
public interface MediaManagementControllerOpenApi {
    @Operation(summary = "List files (admin)", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<Page<MediaFile>> listFiles(MediaFileFilterRequest filter, Pageable pageable);

    @Operation(summary = "Delete file (admin)", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<Void> deleteFile(@PathVariable String uploadId);

    @Operation(summary = "List my files", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<Page<MediaFileResponse>> listMyFiles(MediaFileFilterRequest filter, Pageable pageable, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Delete my file", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<Void> deleteMyFile(@PathVariable String uploadId, @AuthenticationPrincipal Jwt jwt);
}

