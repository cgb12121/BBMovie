package com.bbmovie.fileservice.controller;

import com.bbmovie.fileservice.service.upload.FileUploadStrategyService;
import com.bbmovie.fileservice.service.upload.LocalDiskUploadService;
import com.bbmovie.fileservice.service.upload.UploadStrategyServiceFactory;
import com.example.common.dtos.kafka.UploadMetadata;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@RestController
@RequestMapping("/file")
public class FileUploadController {

    private final LocalDiskUploadService localDiskUploadService;
    private final UploadStrategyServiceFactory uploadStrategyServiceFactory;

    @Autowired
    public FileUploadController(
            LocalDiskUploadService localDiskUploadService,
            UploadStrategyServiceFactory  uploadStrategyServiceFactory
    ) {
        this.localDiskUploadService = localDiskUploadService;
        this.uploadStrategyServiceFactory = uploadStrategyServiceFactory;
    }

    @PostMapping(
            value = "/upload/v1",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Mono<ResponseEntity<String>> handleUploadWithoutRealTimeProgress(
            @RequestPart("file") FilePart filePart,
            @Valid @RequestPart("metadata") UploadMetadata metadata,
            Authentication auth
    ) {
        return localDiskUploadService.uploadFile(filePart, metadata, auth);
    }

    @PostMapping(
            value = "/upload/v2",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> handleUpload(
            @RequestPart("file") FilePart filePart,
            @Valid @RequestPart("metadata") UploadMetadata metadata,
            Authentication auth
    ) {
        return localDiskUploadService.uploadWithProgress(filePart, metadata, auth)
                .map(message -> ServerSentEvent.builder(message).build());
    }

    @PostMapping(
            value = "/upload/test",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Mono<ResponseEntity<String>> upload(
            @RequestPart("file") FilePart filePart,
            @Valid @RequestPart("metadata") UploadMetadata metadata,
            Authentication auth
    ) {
        FileUploadStrategyService strategyService = uploadStrategyServiceFactory.getService(metadata.getStorage());
        return strategyService.uploadFile(filePart, metadata, auth);
    }
}
