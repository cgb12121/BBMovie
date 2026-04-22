package bbmovie.commerce.subscription_service.infrastructure.persistence.repo;

import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.SubscriptionEventInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionEventInboxRepository extends JpaRepository<SubscriptionEventInboxEntity, String> {
    boolean existsByEventId(String eventId);
}
