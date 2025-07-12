package com.example.bbmovieuploadfile.service;

import com.example.common.dtos.kafka.FileUploadResult;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.io.File;

public interface FileStorageStrategy {
    Mono<FileUploadResult> store(FilePart filePart, String safeName);

    Mono<FileUploadResult> store(File file, String filename);

    String getStorageType();
}