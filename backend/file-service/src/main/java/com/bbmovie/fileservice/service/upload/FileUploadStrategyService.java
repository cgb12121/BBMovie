package com.bbmovie.fileservice.service.upload;

import com.example.common.dtos.kafka.UploadMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileUploadStrategyService {
    Mono<ResponseEntity<String>> uploadFileAndTranscodeV1(FilePart filePart, UploadMetadata metadata, Authentication auth);

    Flux<String> uploadFileAndTranscodeWithProgressV2(FilePart filePart, UploadMetadata metadata, Authentication auth);
}