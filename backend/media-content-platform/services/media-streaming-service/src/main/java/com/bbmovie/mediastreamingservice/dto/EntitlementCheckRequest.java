package com.bbmovie.mediastreamingservice.dto;

public record EntitlementCheckRequest(
        String userId,
        String resourceId,
        String action,
        String contentPackage
) {
}
