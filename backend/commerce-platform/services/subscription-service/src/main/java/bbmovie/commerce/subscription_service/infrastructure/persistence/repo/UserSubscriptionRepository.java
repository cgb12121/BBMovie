package bbmovie.commerce.subscription_service.infrastructure.persistence.repo;

import bbmovie.commerce.subscription_service.domain.SubscriptionStatus;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.UserSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, String> {
    List<UserSubscriptionEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<UserSubscriptionEntity> findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
            String userId,
            SubscriptionStatus status,
            Instant now
    );

    Optional<UserSubscriptionEntity> findFirstBySourcePaymentIdOrderByCreatedAtDesc(String sourcePaymentId);
}
