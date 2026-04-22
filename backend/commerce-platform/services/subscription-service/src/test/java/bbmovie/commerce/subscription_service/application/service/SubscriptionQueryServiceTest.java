package bbmovie.commerce.subscription_service.application.service;

import bbmovie.commerce.subscription_service.domain.SubscriptionStatus;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.UserSubscriptionEntity;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.UserSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionQueryServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @InjectMocks
    private SubscriptionQueryService queryService;

    @Test
    void should_return_active_subscription_for_user() {
        UserSubscriptionEntity entity = new UserSubscriptionEntity();
        entity.setSubscriptionId("sub-1");
        entity.setUserId("user-1");
        entity.setPlanId("plan-monthly");
        entity.setStatus(SubscriptionStatus.ACTIVE);
        entity.setStartsAt(Instant.parse("2026-04-01T00:00:00Z"));
        entity.setEndsAt(Instant.parse("2026-05-01T00:00:00Z"));
        entity.setAutoRenew(true);

        when(userSubscriptionRepository.findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
                eq("user-1"),
                eq(SubscriptionStatus.ACTIVE),
                any(Instant.class)
        )).thenReturn(Optional.of(entity));

        var result = queryService.getActiveByUserId("user-1");
        assertEquals("sub-1", result.subscriptionId());
        assertEquals("user-1", result.userId());
        assertEquals("plan-monthly", result.planId());
    }

    @Test
    void should_throw_when_subscription_not_found() {
        when(userSubscriptionRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> queryService.getBySubscriptionId("missing"));
    }

    @Test
    void should_return_user_history() {
        UserSubscriptionEntity entity = new UserSubscriptionEntity();
        entity.setSubscriptionId("sub-1");
        entity.setUserId("user-1");
        entity.setPlanId("plan-monthly");
        entity.setStatus(SubscriptionStatus.ACTIVE);
        entity.setStartsAt(Instant.parse("2026-04-01T00:00:00Z"));
        entity.setEndsAt(Instant.parse("2026-05-01T00:00:00Z"));
        entity.setAutoRenew(true);

        when(userSubscriptionRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(entity));

        var response = queryService.getByUserId("user-1");
        assertEquals("user-1", response.userId());
        assertEquals(1, response.total());
        assertEquals("sub-1", response.subscriptions().getFirst().subscriptionId());
    }
}
