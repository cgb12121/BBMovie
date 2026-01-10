package com.bbmovie.mediastreamingservice.service.policy;

import com.bbmovie.mediastreamingservice.model.Resolution;
import com.bbmovie.mediastreamingservice.model.SubscriptionTier;

/**
 * Policy interface for determining streaming access based on subscription tier.
 * Different implementations can be used for dev/prod environments.
 */
public interface StreamingPolicy {
    
    /**
     * Gets the maximum allowed resolution for a given subscription tier.
     *
     * @param tier The subscription tier
     * @return The maximum allowed resolution
     */
    Resolution getMaxAllowedResolution(SubscriptionTier tier);
}
