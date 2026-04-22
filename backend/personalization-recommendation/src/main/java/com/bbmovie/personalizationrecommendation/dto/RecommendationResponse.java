package com.bbmovie.personalizationrecommendation.dto;

import java.util.List;
import java.util.UUID;

public record RecommendationResponse(
        UUID userId,
        int limit,
        List<RecommendationItem> items
) {
}

