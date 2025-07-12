package com.example.bbmovieuploadfile.controller;

import com.example.common.dtos.kafka.UploadMetadata;
import com.example.bbmovieuploadfile.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
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

@Log4j2
@RestController
@RequestMapping("/file")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> handleUpload(
            @RequestPart("file") FilePart filePart,
            @Valid @RequestPart("metadata") UploadMetadata metadata,
            Authentication auth
    ) {
        return fileUploadService.uploadFile(filePart, metadata, auth);
    }
}
