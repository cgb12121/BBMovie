package com.bbmovie.mediastreamingservice.service.policy;

import com.bbmovie.mediastreamingservice.model.Resolution;
import com.bbmovie.mediastreamingservice.model.SubscriptionTier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Production streaming policy.
 * Uses the tier's defined maximum resolution.
 * This is the default policy that will be used if DevStreamingPolicy is not active.
 */
@Component
@ConditionalOnMissingBean(DevStreamingPolicy.class)
public class ProductionStreamingPolicy implements StreamingPolicy {

    @Override
    public Resolution getMaxAllowedResolution(SubscriptionTier tier) {
        return tier.getMaxResolution();
    }
}
