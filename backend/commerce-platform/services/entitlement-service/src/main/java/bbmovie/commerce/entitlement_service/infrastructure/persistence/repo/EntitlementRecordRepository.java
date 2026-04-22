package bbmovie.commerce.entitlement_service.infrastructure.persistence.repo;

import bbmovie.commerce.entitlement_service.domain.EntitlementStatus;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntitlementRecordRepository extends JpaRepository<EntitlementRecordEntity, String> {
    List<EntitlementRecordEntity> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<EntitlementRecordEntity> findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
            String userId,
            EntitlementStatus status,
            Instant now
    );

    Optional<EntitlementRecordEntity> findFirstBySourcePaymentIdOrderByUpdatedAtDesc(String sourcePaymentId);
}
