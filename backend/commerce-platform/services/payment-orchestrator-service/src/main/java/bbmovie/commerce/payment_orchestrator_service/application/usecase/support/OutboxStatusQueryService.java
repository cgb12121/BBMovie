package bbmovie.commerce.payment_orchestrator_service.application.usecase.support;

import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxEventEntity;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxStatus;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OutboxStatusQueryService {
    private final OutboxEventRepository outboxEventRepository;

    public OutboxStatus snapshot() {
        long pendingCount = outboxEventRepository.countByStatus(OutboxStatus.PENDING);
        Optional<OutboxEventEntity> oldestPending = outboxEventRepository.findFirstByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        Long oldestAgeSeconds = oldestPending
                .map(OutboxEventEntity::getCreatedAt)
                .map(createdAt -> Duration.between(createdAt, Instant.now()).getSeconds())
                .orElse(null);
        return new OutboxStatus(pendingCount, oldestAgeSeconds);
    }

    public record OutboxStatus(long pendingCount, Long oldestPendingAgeSeconds) {}
}
