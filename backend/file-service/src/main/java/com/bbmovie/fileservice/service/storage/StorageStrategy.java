package com.bbmovie.fileservice.service.storage;

import com.example.common.dtos.kafka.FileUploadResult;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.io.File;

public interface StorageStrategy {
    Mono<FileUploadResult> store(FilePart filePart, String safeName);

    Mono<FileUploadResult> store(File file, String filename);

    String getStorageType();
}