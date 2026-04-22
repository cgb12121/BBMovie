package bbmovie.commerce.entitlement_service.infrastructure.persistence.repo;

import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementEventInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EntitlementEventInboxRepository extends JpaRepository<EntitlementEventInboxEntity, String> {
    boolean existsByEventId(String eventId);

    List<EntitlementEventInboxEntity> findByProcessedAtBetweenOrderByProcessedAtAsc(Instant from, Instant to);
}
