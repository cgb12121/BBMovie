package com.example.bbmovieuploadfile.service;

import com.example.common.dtos.kafka.FileUploadResult;
import reactor.core.publisher.Mono;

import java.io.File;

public interface FileCapableStorageStrategy extends FileStorageStrategy {
    Mono<FileUploadResult> store(File file, String safeName);
}
