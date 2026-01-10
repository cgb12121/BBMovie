package com.bbmovie.mediastreamingservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing user subscription tiers.
 * Each tier has a maximum allowed resolution.
 */
@Getter
@RequiredArgsConstructor
public enum SubscriptionTier {
    FREE(Resolution.P480),           // FREE users can access up to 480p
    STANDARD(Resolution.P1080),       // STANDARD users can access up to 1080p
    PREMIUM(Resolution.P2160);       // PREMIUM users can access up to 2160p (4K)

    private final Resolution maxResolution;

    /**
     * Checks if this tier can access a specific resolution.
     *
     * @param resolution The resolution to check
     * @return true if the tier can access this resolution
     */
    public boolean canAccess(Resolution resolution) {
        return resolution.getHeight() <= this.maxResolution.getHeight();
    }

    /**
     * Parses a tier string to a SubscriptionTier enum.
     * Defaults to FREE if the tier is not recognized.
     *
     * @param tier The tier string
     * @return The corresponding SubscriptionTier enum, or FREE if not recognized
     */
    public static SubscriptionTier fromString(String tier) {
        if (tier == null || tier.isBlank()) {
            return FREE;
        }
        
        try {
            return valueOf(tier.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FREE; // Default to FREE if tier is not recognized
        }
    }
}
