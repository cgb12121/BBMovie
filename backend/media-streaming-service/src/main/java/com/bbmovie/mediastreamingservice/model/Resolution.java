package com.bbmovie.mediastreamingservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Enum representing video resolutions.
 * Resolution names (e.g., "1080p") refer to the height in pixels.
 */
@Getter
@RequiredArgsConstructor
public enum Resolution {
    P144(144),
    P240(240),
    P360(360),
    P480(480),
    P720(720),
    P1080(1080),
    P1440(1440),
    P2160(2160),
    P4080(4080);

    private final int height;

    /**
     * Parses a resolution string (e.g., "1080p" or "1080") to a Resolution enum.
     *
     * @param text The resolution string
     * @return The corresponding Resolution enum
     * @throws IllegalArgumentException if the resolution is not recognized
     */
    public static Resolution fromString(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Resolution cannot be null or blank");
        }
        
        // Remove "p" suffix if present and parse height
        String heightStr = text.toLowerCase().replace("p", "").trim();
        try {
            int height = Integer.parseInt(heightStr);
            return Arrays.stream(values())
                    .filter(r -> r.height == height)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown resolution: " + text));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid resolution format: " + text, e);
        }
    }

    /**
     * Returns the resolution string format (e.g., "1080p").
     */
    public String toResolutionString() {
        return height + "p";
    }
}
