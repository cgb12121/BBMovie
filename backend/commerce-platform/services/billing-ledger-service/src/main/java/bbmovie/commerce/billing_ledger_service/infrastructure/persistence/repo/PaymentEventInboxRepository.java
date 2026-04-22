package bbmovie.commerce.billing_ledger_service.infrastructure.persistence.repo;

import bbmovie.commerce.billing_ledger_service.infrastructure.persistence.entity.PaymentEventInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventInboxRepository extends JpaRepository<PaymentEventInboxEntity, Long> {
    boolean existsByEventId(String eventId);
}
