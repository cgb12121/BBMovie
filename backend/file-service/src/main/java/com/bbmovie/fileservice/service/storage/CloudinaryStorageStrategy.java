package com.bbmovie.fileservice.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.bbmovie.common.dtos.nats.FileUploadResult;
import com.bbmovie.common.enums.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Component("cloudinaryStorage")
public class CloudinaryStorageStrategy implements StorageStrategy {

    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryStorageStrategy(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public Mono<FileUploadResult> store(File file, String filename) {
         return Mono.fromCallable(() -> {
            try {
                Map<?, ?> result = cloudinary.uploader().upload(
                    file,
                    ObjectUtils.asMap(
                            "public_id", filename,
                            "resource_type", "auto",
                            "access_mode", "authenticated"
                    )
                );
                String contentType = result.get("resource_type") + "/" + result.get("format");
                long fileSize = ((Number) result.get("bytes")).longValue();
                return new FileUploadResult((String) result.get("secure_url"), (String) result.get("public_id"), contentType, fileSize);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file to Cloudinary", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> delete(String pathOrPublicId) {
        return Mono.fromCallable(() -> {
            try {
                // In Cloudinary, deletion uses public_id
                cloudinary.uploader().destroy(pathOrPublicId, ObjectUtils.emptyMap());
                return null;
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete file from Cloudinary", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public String getStorageType() {
        return Storage.CLOUDINARY.name();
    }
}