package bbmovie.ai_platform.ai_assets.service;

import bbmovie.ai_platform.ai_assets.dto.AssetPresignRequest;
import bbmovie.ai_platform.ai_assets.dto.AssetPresignResponse;
import bbmovie.ai_platform.ai_assets.entity.Asset;
import bbmovie.ai_platform.ai_assets.repository.AssetRepository;
import bbmovie.ai_platform.ai_common.enums.AssetStatus;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name:ai-platform-assets}")
    private String bucketName;

    /**
     * Generates a pre-signed URL for file upload and creates a record in DB.
     */
    public Mono<AssetPresignResponse> generatePresignedUrl(UUID userId, AssetPresignRequest request) {
        UUID assetId = UUID.randomUUID();
        String objectKey = String.format("%s/%s/%s", userId, assetId, request.fileName());

        return Mono.fromCallable(() -> {
            log.debug("[AssetService] Generating presigned URL for key: {}", objectKey);
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry(15, TimeUnit.MINUTES)
                    .build()
            );
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(url -> {
            Asset asset = Asset.builder()
                .id(assetId)
                .userId(userId)
                .bucketName(bucketName)
                .objectKey(objectKey)
                .contentType(request.contentType())
                .status(AssetStatus.UPLOADING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            return assetRepository.save(asset)
                .thenReturn(new AssetPresignResponse(assetId, url));
        });
    }

    public Mono<Asset> getAsset(UUID assetId) {
        return assetRepository.findById(assetId);
    }
}
