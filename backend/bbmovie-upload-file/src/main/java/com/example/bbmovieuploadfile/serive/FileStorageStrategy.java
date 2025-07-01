package com.example.bbmovieuploadfile.serive;

import com.example.common.dtos.kafka.FileUploadResult;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface FileStorageStrategy {
    Mono<FileUploadResult> store(FilePart filePart, String safeName);

    String getStorageType();
}