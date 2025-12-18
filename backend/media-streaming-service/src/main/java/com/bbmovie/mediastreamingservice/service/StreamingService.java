package com.bbmovie.mediastreamingservice.service;

import com.bbmovie.mediastreamingservice.exception.InaccessibleFileException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.hls}")
    private String hlsBucket;

    @Value("${minio.bucket.secure}")
    private String secureBucket;

    public InputStreamResource getHlsFile(UUID movieId, String resolution) {
        String objectKey = "movies/" + movieId + "/" + resolution + "/playlist.m3u8";
        return getFile(hlsBucket, objectKey);
    }

    public InputStreamResource getMasterPlaylist(UUID movieId) {
        String objectKey = "movies/" + movieId.toString() + "/master.m3u8";
        return getFile(hlsBucket, objectKey);
    }

    public InputStreamResource getSecureKey(UUID movieId, String resolution, String keyFile) {
        String objectKey = "movies/" + movieId + "/" + resolution + "/" + keyFile;
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
            throw new InaccessibleFileException("File not found or inaccessible: " + objectKey);
        }
    }
}
