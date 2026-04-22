package bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa;

import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxEventEntity;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {
    List<OutboxEventEntity> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(OutboxStatus status, Instant now);
    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE status = :status
              AND next_attempt_at <= :now
            ORDER BY created_at ASC
            LIMIT :limitRows
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventEntity> claimPendingBatch(@Param("status") String status,
                                              @Param("now") Instant now,
                                              @Param("limitRows") int limitRows);
    long countByStatus(OutboxStatus status);
    Optional<OutboxEventEntity> findFirstByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
