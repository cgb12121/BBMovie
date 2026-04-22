package bbmovie.commerce.subscription_service.adapter.inbound.rest.dto;

import java.util.List;

public record UserSubscriptionsResponse(
        String userId,
        int total,
        List<SubscriptionResponse> subscriptions
) {
}
