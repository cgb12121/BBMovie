package com.bbmovie.fileservice.controller;

import com.bbmovie.fileservice.service.upload.FileUploadService;
import com.bbmovie.common.dtos.nats.UploadMetadata;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/image")
public class ImageUploadController {

    private final FileUploadService fileUploadService;

    @Autowired
    public ImageUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Mono<ResponseEntity<String>> handleUpload(
            @RequestPart("file") FilePart filePart,
            @Valid @RequestPart("metadata") UploadMetadata metadata,
            Authentication auth) {
        return fileUploadService.orchestrateUpload(filePart, metadata, auth);
    }
}
