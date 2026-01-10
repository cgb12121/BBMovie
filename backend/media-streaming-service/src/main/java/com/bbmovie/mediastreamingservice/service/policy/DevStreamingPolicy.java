package com.bbmovie.mediastreamingservice.service.policy;

import com.bbmovie.mediastreamingservice.model.Resolution;
import com.bbmovie.mediastreamingservice.model.SubscriptionTier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Development streaming policy.
 * Applies stricter restrictions to save bandwidth and storage during development.
 * - FREE users: 144p only
 * - STANDARD users: 240p
 * - PREMIUM users: 240p only (for debugging)
 * Active in dev/default/local/docker profiles.
 */
@Component
@Profile({"dev", "default", "local", "docker"})
public class DevStreamingPolicy implements StreamingPolicy {

    @Override
    public Resolution getMaxAllowedResolution(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> Resolution.P144;      // Dev: FREE gets 144p only
            case STANDARD -> Resolution.P240;   // Dev: STANDARD gets 240p
            case PREMIUM -> Resolution.P2160;    // Dev: PREMIUM gets 2160p for debugging
        };
    }
}
