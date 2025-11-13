package com.bbmovie.ai_assistant_service.core.low_level._utils;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for sanitizing AI thinking content to prevent leaking sensitive information.
 * 
 * <p>This class removes or masks:
 * <ul>
 *   <li>Session UUIDs and other UUIDs</li>
 *   <li>Internal tool/function names</li>
 *   <li>System implementation details</li>
 *   <li>Internal parameter names</li>
 * </ul>
 */
public class _ThinkingSanitizer {

    // Pattern to match UUIDs (8-4-4-4-12 hex format)
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern to match session IDs in various formats
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile(
        "(?i)(session[\\s_-]?id|sessionId|session_id)[\\s:=]+([0-9a-f-]{36}|[a-z0-9-]+)",
        Pattern.CASE_INSENSITIVE
    );

    // Internal tool/function names that should be masked
    private static final String[] INTERNAL_TOOLS = {
        "retrieve_rag_movies",
        "getSystemUsageStats",
        "broadcastMessage",
        "viewUserDetails",
        "manageUserAccount",
        "viewAuditTrail",
        "sessionId",
        "queryMovies",
        "topK"
    };

    // Internal system terms that should be masked
    private static final String[] SYSTEM_TERMS = {
        "function",
        "tool",
        "parameter",
        "available tools",
        "provided tools",
        "system would",
        "system internals"
    };

    /**
     * Sanitizes thinking content by removing or masking sensitive information.
     * 
     * @param thinking the raw thinking content from the AI
     * @return sanitized thinking content with sensitive information removed/masked
     */
    public static String sanitize(String thinking) {
        if (thinking == null || thinking.isBlank()) {
            return thinking;
        }

        String sanitized = thinking;

        // Remove or mask UUIDs
        sanitized = UUID_PATTERN.matcher(sanitized).replaceAll("[SESSION_ID]");

        // Remove session ID references
        sanitized = SESSION_ID_PATTERN.matcher(sanitized).replaceAll("session: [SESSION_ID]");

        // Mask internal tool/function names
        for (String tool : INTERNAL_TOOLS) {
            sanitized = sanitized.replaceAll("(?i)\\b" + Pattern.quote(tool) + "\\b", "[TOOL]");
        }

        // Remove detailed system implementation information
        sanitized = removeSystemDetails(sanitized);

        // Clean up multiple spaces and normalize
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        return sanitized;
    }

    /**
     * Removes system implementation details while preserving general reasoning.
     */
    private static String removeSystemDetails(String text) {
        // Remove sentences that contain too much system detail
        String[] sentences = text.split("[.!?]+");
        StringBuilder cleaned = new StringBuilder();

        for (String sentence : sentences) {
            String lowerSentence = sentence.toLowerCase().trim();
            
            // Skip sentences that reveal too much system internals
            boolean shouldSkip = false;
            for (String term : SYSTEM_TERMS) {
                if (lowerSentence.contains(term.toLowerCase()) && 
                    (lowerSentence.contains("available") || 
                     lowerSentence.contains("provided") ||
                     lowerSentence.contains("require") ||
                     lowerSentence.contains("parameter"))) {
                    shouldSkip = true;
                    break;
                }
            }

            // Skip sentences that mention specific function parameters
            if (lowerSentence.matches(".*\\b(sessionId|queryMovies|topK|parameter)\\b.*") &&
                (lowerSentence.contains("require") || lowerSentence.contains("missing") || 
                 lowerSentence.contains("provided") || lowerSentence.contains("given"))) {
                shouldSkip = true;
            }

            if (!shouldSkip && !sentence.trim().isEmpty()) {
                cleaned.append(sentence.trim()).append(". ");
            }
        }

        return cleaned.toString().trim();
    }

    /**
     * Completely removes thinking content (for production environments where thinking should be disabled).
     * 
     * @return null to indicate thinking should not be included
     */
    public static String remove() {
        return null;
    }

    /**
     * Checks if thinking should be sanitized based on configuration.
     * 
     * @param enableSanitization whether sanitization is enabled
     * @param thinking the raw thinking content
     * @return sanitized thinking if enabled, original if disabled, null if thinking should be removed
     */
    public static String process(boolean enableSanitization, String thinking) {
        if (thinking == null || thinking.isBlank()) {
            return thinking;
        }

        if (!enableSanitization) {
            return thinking;
        }

        return sanitize(thinking);
    }
}

