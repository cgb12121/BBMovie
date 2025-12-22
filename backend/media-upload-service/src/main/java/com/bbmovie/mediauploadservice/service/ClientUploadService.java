package com.bbmovie.mediauploadservice.service;

import com.bbmovie.mediauploadservice.dto.UploadInitRequest;
import com.bbmovie.mediauploadservice.dto.UploadInitResponse;
import com.bbmovie.mediauploadservice.entity.MediaFile;
import com.bbmovie.mediauploadservice.enums.MediaStatus;
import com.bbmovie.mediauploadservice.enums.StorageProvider;
import com.bbmovie.mediauploadservice.exception.PresignUrlException;
import com.bbmovie.mediauploadservice.repository.MediaFileRepository;
import com.bbmovie.mediauploadservice.enums.UploadPurpose;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.bbmovie.mediauploadservice.exception.InvalidChecksumException;
import com.bbmovie.mediauploadservice.exception.UnsupportedFileTypeException;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ROLE;
import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientUploadService {

    @Value("${minio.bucket.movie-raw:bbmovie-raw}")
    private String rawBucketName;

    @Value("${minio.bucket.ai-assets:bbmovie-ai-assets}")
    private String aiAssetsBucketName;

    private final MinioClient minioClient;
    private final MediaFileRepository mediaFileRepository;
    private final ObjectKeyStrategy objectKeyStrategy;

    @Transactional(readOnly = true)
    public String generateDownloadUrl(String uploadId, Jwt jwt) {
        MediaFile mediaFile = mediaFileRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + uploadId));

        String userId = jwt.getClaim(SUB);
        String role = jwt.getClaim(ROLE); // Assuming simple string claim for now, adjust if list

        if (!mediaFile.getUserId().equals(userId) && !"ADMIN".equals(role)) {
            throw new AccessDeniedException("You are not authorized to access this file.");
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(mediaFile.getBucket())
                            .object(mediaFile.getObjectKey())
                            .expiry(1, TimeUnit.HOURS)
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate download URL", e);
            throw new PresignUrlException("Failed to generate download link.");
        }
    }

    @Transactional
    public UploadInitResponse initUpload(UploadInitRequest request, Jwt jwt) {
        softValidateMediaType(request);

        String uploadId = UUID.randomUUID().toString();
        String userId = jwt.getClaim(SUB);
        // Generate Object Key Minio
        String objectName = objectKeyStrategy.build(request.getPurpose(), uploadId, request.getFilename());
        String expireAt = String.valueOf(
                Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
        );

        // Determine bucket based on purpose
        String bucketName = determineBucket(request.getPurpose());

        // Save to DB
        registerUploadRequest(request, uploadId, userId, objectName, bucketName);

        // Generate Metadata
        Map<String, String> metadata = createS3metadata(request, uploadId, userId, expireAt);

        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .extraQueryParams(metadata)
                            .build());

            return UploadInitResponse.builder()
                    .uploadId(uploadId)
                    .objectKey(objectName)
                    .uploadUrl(url)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate presigned-url, error [{}] {}", e.getClass(), e.getMessage());
            throw new PresignUrlException("Failed to generate upload link.");
        }
    }

    private Map<String, String> createS3metadata(UploadInitRequest request, String uploadId, String userId, String expireTimeString) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("response-content-type", request.getContentType());
        metadata.put("x-amz-meta-video-id", uploadId);
        metadata.put("x-amz-meta-uploader-id", userId);
        metadata.put("x-amz-meta-purpose", request.getPurpose().name());
        metadata.put("x-amz-meta-expire-at", expireTimeString);

        if (request.getChecksum() != null) {
            metadata.put("x-amz-meta-expected-checksum", request.getChecksum());
        } else {
            throw new InvalidChecksumException("Missing checksum");
        }
        return metadata;
    }

    private String determineBucket(UploadPurpose purpose) {
        return switch (purpose) {
            case AI_ASSET -> aiAssetsBucketName;
            case USER_AVATAR, MOVIE_POSTER, MOVIE_TRAILER, MOVIE_SOURCE -> rawBucketName;
        };
    }

    private void registerUploadRequest(UploadInitRequest request, String uploadId, String userId, String objectName, String bucketName) {
        MediaFile mediaFile = MediaFile.builder()
                .uploadId(uploadId)
                .userId(userId)
                .originalFilename(request.getFilename())
                .bucket(bucketName)
                .objectKey(objectName)
                .status(MediaStatus.INITIATED)
                .purpose(request.getPurpose())
                .storageProvider(StorageProvider.MINIO)
                .mimeType(request.getContentType())
                .sizeBytes(request.getSizeBytes())
                .checksum(request.getChecksum())
                .sparseChecksum(request.getSparseChecksum())
                .build();

        mediaFileRepository.save(mediaFile);
    }

    private void softValidateMediaType(UploadInitRequest request) {
        Set<String> allowedTypes = request.getPurpose().getAllowedMimeTypes();
        String contentType = request.getContentType();

        if (!allowedTypes.contains(contentType)) {
            throw new UnsupportedFileTypeException("File type '" + contentType + "' is not supported.");
        }
    }
}
