package bbmovie.commerce.subscription_service.application.service;

import bbmovie.commerce.subscription_service.adapter.inbound.rest.dto.SubscriptionResponse;
import bbmovie.commerce.subscription_service.adapter.inbound.rest.dto.UserSubscriptionsResponse;
import bbmovie.commerce.subscription_service.domain.SubscriptionStatus;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.UserSubscriptionEntity;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionQueryService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    public UserSubscriptionsResponse getByUserId(String userId) {
        List<SubscriptionResponse> subscriptions = userSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new UserSubscriptionsResponse(userId, subscriptions.size(), subscriptions);
    }

    public SubscriptionResponse getBySubscriptionId(String subscriptionId) {
        return userSubscriptionRepository.findById(subscriptionId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subscription not found: " + subscriptionId
                ));
    }

    public SubscriptionResponse getActiveByUserId(String userId) {
        return userSubscriptionRepository
                .findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
                        userId,
                        SubscriptionStatus.ACTIVE,
                        Instant.now()
                )
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("No active subscription found for user: " + userId));
    }

    private SubscriptionResponse toResponse(UserSubscriptionEntity entity) {
        return new SubscriptionResponse(
                entity.getSubscriptionId(),
                entity.getUserId(),
                entity.getPlanId(),
                entity.getCampaignId(),
                entity.getSourcePaymentId(),
                entity.getStatus(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.isAutoRenew()
        );
    }
}
