package bbmovie.ai_platform.ai_assets.listener;

import bbmovie.ai_platform.ai_assets.entity.Asset;
import bbmovie.ai_platform.ai_assets.repository.AssetRepository;
import bbmovie.ai_platform.ai_assets.service.NatsService;
import bbmovie.ai_platform.ai_common.dto.AssetEvent;
import bbmovie.ai_platform.ai_common.dto.MinioEvent;
import bbmovie.ai_platform.ai_common.enums.AssetStatus;
import bbmovie.ai_platform.ai_common.constants.NatsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.stereotype.Component;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import org.apache.tika.Tika;
import java.io.InputStream;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioEventSubscriber {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final AssetRepository assetRepository;
    private final NatsService natsService;
    private final MinioClient minioClient;
    private final Tika tika = new Tika();

    @PostConstruct
    public void subscribe() {
        log.info("[MinioEventSubscriber] Subscribing to: {}", NatsConstants.MINIO_AI_ASSETS_SUBJECT);
        
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            try {
                String payload = new String(msg.getData());
                log.debug("[MinioEventSubscriber] Received MinIO Event: {}", payload);
                
                MinioEvent event = objectMapper.readValue(payload, MinioEvent.class);
                if (event.getRecords() != null) {
                    for (MinioEvent.MinioRecord record : event.getRecords()) {
                        handleRecord(record);
                    }
                }
            } catch (Exception e) {
                log.error("[MinioEventSubscriber] Error processing MinIO event: {}", e.getMessage());
            }
        });

        dispatcher.subscribe(NatsConstants.MINIO_AI_ASSETS_SUBJECT);
    }

    private void handleRecord(MinioEvent.MinioRecord record) {
        String objectKey = record.getS3().getObject().getKey();
        log.info("[MinioEventSubscriber] Processing upload for object: {}", objectKey);

        // Parse assetId from key: userId/assetId/filename
        String[] parts = objectKey.split("/");
        if (parts.length < 3) return;
        
        try {
            UUID assetId = UUID.fromString(parts[1]);
            
            assetRepository.findById(assetId)
                .flatMap(asset -> {
                    // Detect real MIME type from MinIO
                    return detectMimeType(asset)
                        .map(realMimeType -> {
                            log.info("[MinioEventSubscriber] Detected MIME: {} for asset: {}", realMimeType, assetId);
                            asset.setContentType(realMimeType);
                            asset.setStatus(AssetStatus.UPLOADED);
                            asset.setSize(record.getS3().getObject().getSize());
                            return asset;
                        })
                        .flatMap(assetRepository::save);
                })
                .flatMap(asset -> {
                    // Trigger Ingestion
                    AssetEvent trigger = AssetEvent.builder()
                        .assetId(asset.getId())
                        .userId(asset.getUserId())
                        .bucket(asset.getBucketName())
                        .objectKey(asset.getObjectKey())
                        .status(AssetStatus.UPLOADED)
                        .build();
                        
                    return natsService.publishAssetEvent(NatsConstants.ASSET_PROCESS_TRIGGER, trigger);
                })
                .subscribe(
                    v -> log.info("[MinioEventSubscriber] Successfully updated status and triggered ingestion for asset: {}", assetId),
                    err -> log.error("[MinioEventSubscriber] Failed to handle asset {}: {}", assetId, err.getMessage())
                );
                
        } catch (IllegalArgumentException e) {
            log.warn("[MinioEventSubscriber] Invalid assetId in key: {}", objectKey);
        }
    }

    private Mono<String> detectMimeType(Asset asset) {
        return Mono.fromCallable(() -> {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(asset.getBucketName())
                            .object(asset.getObjectKey())
                            .build())) {
                return tika.detect(stream);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
