package bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa;

import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {
    List<OutboxEventEntity> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(String status, Instant now);
    long countByStatus(String status);
    Optional<OutboxEventEntity> findFirstByStatusOrderByCreatedAtAsc(String status);
}
