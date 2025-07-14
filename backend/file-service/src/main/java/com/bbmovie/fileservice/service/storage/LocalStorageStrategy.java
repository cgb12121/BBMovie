package com.bbmovie.fileservice.service.storage;

import com.example.common.dtos.kafka.FileUploadResult;
import reactor.core.publisher.Mono;

import java.io.File;

public interface LocalStorageStrategy extends StorageStrategy {
    Mono<FileUploadResult> store(File file, String safeName);
}
