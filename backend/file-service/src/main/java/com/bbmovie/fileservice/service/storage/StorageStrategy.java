package com.bbmovie.fileservice.service.storage;

import com.bbmovie.common.dtos.nats.FileUploadResult;
import reactor.core.publisher.Mono;

import java.io.File;

public interface StorageStrategy {
    Mono<FileUploadResult> store(File file, String filename);
    Mono<Void> delete(String pathOrPublicId); // Added delete support
    String getStorageType();
}
