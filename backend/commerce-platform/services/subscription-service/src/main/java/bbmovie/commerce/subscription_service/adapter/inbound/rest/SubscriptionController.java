package bbmovie.commerce.subscription_service.adapter.inbound.rest;

import bbmovie.commerce.subscription_service.adapter.inbound.rest.dto.SubscriptionResponse;
import bbmovie.commerce.subscription_service.adapter.inbound.rest.dto.UserSubscriptionsResponse;
import bbmovie.commerce.subscription_service.application.service.SubscriptionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionQueryService subscriptionQueryService;

    @GetMapping("/user/{userId}")
    public UserSubscriptionsResponse getByUserId(@PathVariable String userId) {
        return subscriptionQueryService.getByUserId(userId);
    }

    @GetMapping("/{subscriptionId}")
    public SubscriptionResponse getBySubscriptionId(@PathVariable String subscriptionId) {
        return subscriptionQueryService.getBySubscriptionId(subscriptionId);
    }

    @GetMapping("/user/{userId}/active")
    public SubscriptionResponse getActiveByUserId(@PathVariable String userId) {
        return subscriptionQueryService.getActiveByUserId(userId);
    }
}
