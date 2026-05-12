package bbmovie.ai_platform.ai_assets.service;

import bbmovie.ai_platform.ai_common.dto.AssetEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NatsService {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    /**
     * Publishes an asset event to NATS JetStream.
     */
    public Mono<Void> publishAssetEvent(String subject, AssetEvent event) {
        return Mono.fromRunnable(() -> {
            try {
                JetStream js = natsConnection.jetStream();
                byte[] payload = objectMapper.writeValueAsBytes(event);
                
                log.debug("[NATS] Publishing event to {}: {}", subject, event);
                js.publish(subject, payload);
            } catch (Exception e) {
                log.error("[NATS] Failed to publish event to {}: {}", subject, e.getMessage());
                throw new RuntimeException("NATS Publish Error", e);
            }
        }).then();
    }
}
