package com.bbmovie.fileservice.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.common.dtos.nats.FileUploadResult;
import com.example.common.enums.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Component("cloudinaryStorage")
public class CloudinaryStorageStrategy implements StorageStrategy {

    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryStorageStrategy(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public Mono<FileUploadResult> store(FilePart filePart, String safeName) {
        return Mono.fromCallable(() -> {
            try {
                Map<?, ?> result = cloudinary.uploader().upload(
                    DataBufferUtils.join(filePart.content()).block().asInputStream(true),
                    ObjectUtils.asMap(
                            "public_id", safeName,
                            "resource_type", "auto",
                            "access_mode", "authenticated"
                    )
                );
                String contentType = result.get("resource_type") + "/" + result.get("format");
                long fileSize = ((Number) result.get("bytes")).longValue();
                return new FileUploadResult((String) result.get("secure_url"), (String) result.get("public_id"), contentType, fileSize);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload to Cloudinary", e);
            }
        });
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
        });
    }

    @Override
    public String getStorageType() {
        return Storage.CLOUDINARY.name();
    }
}
