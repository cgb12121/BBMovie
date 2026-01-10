package com.bbmovie.mediastreamingservice.service;

import com.bbmovie.mediastreamingservice.exception.AccessDeniedException;
import com.bbmovie.mediastreamingservice.model.Resolution;
import com.bbmovie.mediastreamingservice.model.SubscriptionTier;
import com.bbmovie.mediastreamingservice.service.policy.StreamingPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for access control and playlist filtering.
 * Handles authorization checks and filtering based on subscription tiers.
 * This service does NOT depend on StreamingService to avoid circular dependencies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingAccessControlService {

    private final StreamingPolicy streamingPolicy;

    /**
     * Regex pattern to extract resolution from the HLS stream info line.
     * Matches: RESOLUTION=1920x1080 and captures the height (1080).
     */
    private static final Pattern RESOLUTION_PATTERN = Pattern.compile("RESOLUTION=(\\d+)x(\\d+)");

    /**
     * Filters the master playlist content based on the user subscription tier.
     *
     * @param content The original master playlist content
     * @param tierStr The user's subscription tier string
     * @return Filtered master playlist content as a string
     */
    public String filterMasterPlaylist(String content, String tierStr) {
        SubscriptionTier tier = SubscriptionTier.fromString(tierStr);
        Resolution maxAllowed = streamingPolicy.getMaxAllowedResolution(tier);
        
        String filteredContent = filterPlaylistByResolution(content, maxAllowed);
        
        log.info("Filtered master playlist - Tier: {}, MaxResolution: {}", tier, maxAllowed.toResolutionString());
        log.debug("Filtered playlist content (first 500 chars): {}", 
                filteredContent.length() > 500 ? filteredContent.substring(0, 500) + "..." : filteredContent);
        
        return filteredContent;
    }

    /**
     * Checks if a user has access to a specific resolution and throws an exception if denied.
     *
     * @param tierStr       The user's subscription tier string
     * @param resolutionStr The resolution string (e.g., "720p", "1080p")
     * @throws AccessDeniedException if the user doesn't have access to this resolution
     */
    public void checkAccessToResolution(String tierStr, String resolutionStr) {
        try {
            SubscriptionTier tier = SubscriptionTier.fromString(tierStr);
            Resolution requestedRes = Resolution.fromString(resolutionStr);
            Resolution maxAllowed = streamingPolicy.getMaxAllowedResolution(tier);
            
            if (requestedRes.getHeight() > maxAllowed.getHeight()) {
                String message = String.format("Access denied: Tier %s (max: %s) cannot access %s", 
                        tier, maxAllowed.toResolutionString(), resolutionStr);
                log.warn(message);
                throw new AccessDeniedException(message);
            }
        } catch (IllegalArgumentException e) {
            String message = String.format("Invalid tier or resolution: tier=%s, resolution=%s", 
                    tierStr, resolutionStr);
            log.warn(message, e);
            throw new AccessDeniedException(message, e);
        }
    }

    /**
     * Filters the master playlist content based on the maximum allowed resolution.
     * Uses proper regex to extract resolution from HLS stream info lines.
     *
     * @param content    The original master playlist content
     * @param maxAllowed The maximum allowed resolution
     * @return Filtered playlist content
     */
    private String filterPlaylistByResolution(String content, Resolution maxAllowed) {
        // If max allowed is the highest resolution, return original content
        Resolution highestRes = Arrays.stream(Resolution.values())
                .max(Comparator.comparingInt(Resolution::getHeight))
                .orElse(Resolution.P4080);
        
        if (maxAllowed.getHeight() >= highestRes.getHeight()) {
            return content;
        }

        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        boolean skipNextUrl = false;

        for (String line : lines) {
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                Resolution streamRes = extractResolution(line);
                // If resolution is higher than max allowed, filter it out
                if (streamRes != null && streamRes.getHeight() > maxAllowed.getHeight()) {
                    skipNextUrl = true; // Mark to skip the URL line that follows
                    continue; // Skip this stream info line
                }
                skipNextUrl = false; // Reset if this stream is allowed
            } else if (!line.startsWith("#") && skipNextUrl) {
                // This is the URL line for a filtered stream - skip it
                continue;
            }

            result.append(line).append("\n");
        }

        String filteredContent = result.toString();
        log.debug("Filtered playlist content (max allowed: {}):\n{}", maxAllowed.toResolutionString(), filteredContent);
        return filteredContent;
    }

    /**
     * Extracts resolution from an HLS stream info line using regex.
     * Parses the RESOLUTION=WIDTHxHEIGHT attribute and returns the Resolution enum.
     *
     * @param streamInfoLine The #EXT-X-STREAM-INF line containing resolution info
     * @return The Resolution enum, or null if not found or invalid
     */
    private Resolution extractResolution(String streamInfoLine) {
        Matcher matcher = RESOLUTION_PATTERN.matcher(streamInfoLine);
        if (matcher.find()) {
            try {
                // Group 2 is the height (e.g., 1080 from "1920x1080")
                int height = Integer.parseInt(matcher.group(2));
                
                // Find matching Resolution enum
                return Arrays.stream(Resolution.values())
                        .filter(r -> r.getHeight() == height)
                        .findFirst()
                        .orElse(null);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse resolution height from line: {}", streamInfoLine);
                return null;
            }
        }
        return null;
    }
}
