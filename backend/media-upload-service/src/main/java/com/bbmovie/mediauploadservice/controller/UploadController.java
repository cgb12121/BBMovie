package com.bbmovie.mediauploadservice.controller;

import com.bbmovie.mediauploadservice.dto.UploadInitRequest;
import com.bbmovie.mediauploadservice.dto.UploadInitResponse;
import com.bbmovie.mediauploadservice.service.ClientUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final ClientUploadService clientUploadService;

    //presign for the client to upload
    @PostMapping("/init")
    public ResponseEntity<UploadInitResponse> initUpload(@RequestBody @Valid UploadInitRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(clientUploadService.initUpload(request, jwt));
    }

    @GetMapping("/files/{uploadId}/url")
    public ResponseEntity<java.util.Map<String, String>> getDownloadUrl(@PathVariable String uploadId, @AuthenticationPrincipal Jwt jwt) {
        String url = clientUploadService.generateDownloadUrl(uploadId, jwt);
        return ResponseEntity.ok(java.util.Map.of("downloadUrl", url));
    }
}
