package com.example.bbmovieuploadfile.serive;

import com.cloudinary.Cloudinary;
import com.example.common.dtos.kafka.FileUploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;

@Component("cloudinaryStorage")
public class CloudinaryFileStorageStrategy implements FileStorageStrategy {

    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryFileStorageStrategy(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public Mono<FileUploadResult> store(FilePart filePart, String safeName) {
        return filePart.transferTo(Paths.get("/tmp/" + safeName)) // temp save
                .then(Mono.fromCallable(() -> {
                    Map<?, ?> result = cloudinary.uploader().upload(
                        new File("/tmp/" + safeName),
                        Map.of("public_id", safeName)
                    );
                    return new FileUploadResult((String) result.get("secure_url"), (String) result.get("public_id"));
                }));
    }

    @Override
    public String getStorageType() {
        return "cloudinaryStorage";
    }
}
