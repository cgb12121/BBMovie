package bbmovie.commerce.entitlement_service.application.service;

import bbmovie.commerce.entitlement_service.application.dto.PaymentEventEnvelope;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementEventInboxEntity;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementEventInboxRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementReplayService {

    private final EntitlementEventInboxRepository inboxRepository;
    private final EntitlementProjectionService projectionService;
    private final ObjectMapper objectMapper;

    @Value("${entitlement.replay.enabled:false}")
    private boolean replayEnabled;

    public int replayRange(Instant from, Instant to) {
        List<EntitlementEventInboxEntity> rows = inboxRepository.findByProcessedAtBetweenOrderByProcessedAtAsc(from, to);
        int replayed = 0;
        for (EntitlementEventInboxEntity row : rows) {
            try {
                PaymentEventEnvelope envelope = objectMapper.readValue(row.getRawEventJson(), PaymentEventEnvelope.class);
                projectionService.ingest("replay-" + row.getEventId(), envelope, true);
                replayed++;
            } catch (Exception ex) {
                log.warn("Failed replay for eventId={}", row.getEventId(), ex);
            }
        }
        return replayed;
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void scheduledReplay() {
        if (!replayEnabled) {
            return;
        }
        Instant to = Instant.now();
        Instant from = to.minus(60, ChronoUnit.MINUTES);
        int replayed = replayRange(from, to);
        if (replayed > 0) {
            log.info("Scheduled replay processed {} entitlement events", replayed);
        }
    }
}
