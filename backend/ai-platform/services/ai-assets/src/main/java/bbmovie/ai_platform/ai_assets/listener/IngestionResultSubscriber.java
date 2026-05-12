package bbmovie.ai_platform.ai_assets.listener;

import bbmovie.ai_platform.ai_assets.entity.IngestedContent;
import bbmovie.ai_platform.ai_assets.repository.AssetRepository;
import bbmovie.ai_platform.ai_assets.repository.IngestedContentRepository;
import bbmovie.ai_platform.ai_common.constants.NatsConstants;
import bbmovie.ai_platform.ai_common.dto.AssetEvent;
import bbmovie.ai_platform.ai_common.enums.AssetStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionResultSubscriber {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final AssetRepository assetRepository;
    private final IngestedContentRepository contentRepository;

    @PostConstruct
    public void subscribe() {
        log.info("[IngestionResultSubscriber] Subscribing to: {}", NatsConstants.ASSET_PROCESS_COMPLETED);

        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            try {
                // Assuming Rust sends the same AssetEvent structure but with content
                // Or we can create a specific IngestionResultDto
                AssetEvent event = objectMapper.readValue(msg.getData(), AssetEvent.class);
                handleCompletion(event);
            } catch (Exception e) {
                log.error("[IngestionResultSubscriber] Error processing completion event: {}", e.getMessage());
            }
        });

        dispatcher.subscribe(NatsConstants.ASSET_PROCESS_COMPLETED);
    }

    private void handleCompletion(AssetEvent event) {
        log.info("[IngestionResultSubscriber] Received completion for asset: {}", event.getAssetId());

        // 1. Update Asset Status to INGESTED
        assetRepository.findById(event.getAssetId())
                .flatMap(asset -> {
                    asset.setStatus(AssetStatus.INGESTED);
                    return assetRepository.save(asset);
                })
                .subscribe();

        // 2. Save Ingested Content
        if (event.getContent() != null) {
            IngestedContent content = IngestedContent.builder()
                    .id(UUID.randomUUID())
                    .assetId(event.getAssetId())
                    .content(event.getContent())
                    .wordCount(event.getContent().split("\\s+").length)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            contentRepository.findByAssetId(event.getAssetId())
                    .switchIfEmpty(contentRepository.save(content))
                    .subscribe();
        }
    }
}
