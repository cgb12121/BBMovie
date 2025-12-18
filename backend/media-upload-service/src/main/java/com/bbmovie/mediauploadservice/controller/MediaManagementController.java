package com.bbmovie.mediauploadservice.controller;

import com.bbmovie.mediauploadservice.dto.MediaFileFilterRequest;
import com.bbmovie.mediauploadservice.dto.MediaFileResponse;
import com.bbmovie.mediauploadservice.entity.MediaFile;
import com.bbmovie.mediauploadservice.mapper.MediaFileMapper;
import com.bbmovie.mediauploadservice.service.MediaManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;

@RestController
@RequestMapping("/management/files")
@RequiredArgsConstructor
public class MediaManagementController {

    private final MediaManagementService mediaManagementService;
    private final MediaFileMapper mediaFileMapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<MediaFile>> listFiles(
            MediaFileFilterRequest filter,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(mediaManagementService.listMediaFiles(filter, pageable));
    }

    @DeleteMapping("/{uploadId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFile(@PathVariable String uploadId) {
        mediaManagementService.deleteMediaFile(uploadId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-files")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MediaFileResponse>> listMyFiles(
            MediaFileFilterRequest filter,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaim(SUB);
        filter.setUserId(userId); // Force userId to current user
        Page<MediaFileResponse> page = mediaManagementService.listMediaFiles(filter, pageable)
                .map(mediaFileMapper::toResponse);
        return ResponseEntity.ok(page);
    }

    @DeleteMapping("/my-files/{uploadId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyFile(@PathVariable String uploadId, @AuthenticationPrincipal Jwt jwt) {
        mediaManagementService.deleteMediaFile(uploadId, jwt);
        return ResponseEntity.noContent().build();
    }
}
