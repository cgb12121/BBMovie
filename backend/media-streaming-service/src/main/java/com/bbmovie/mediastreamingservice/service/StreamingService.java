package com.bbmovie.mediastreamingservice.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.hls}")
    private String hlsBucket;

    @Value("${minio.bucket.secure}")
    private String secureBucket;

    public InputStreamResource getHlsFile(String objectKey) {
        return getFile(hlsBucket, objectKey);
    }

    public InputStreamResource getSecureKey(String objectKey) {
        return getFile(secureBucket, objectKey);
    }

    private InputStreamResource getFile(String bucket, String objectKey) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
            return new InputStreamResource(stream);
        } catch (Exception e) {
            log.error("Failed to fetch file {} from bucket {}", objectKey, bucket, e);
            throw new RuntimeException("File not found or inaccessible: " + objectKey);
        }
    }
}
