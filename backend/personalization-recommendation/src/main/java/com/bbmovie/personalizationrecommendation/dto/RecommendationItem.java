package com.bbmovie.personalizationrecommendation.dto;

import java.util.UUID;

public record RecommendationItem(
        UUID movieId,
        double score,
        String reason
) {
}

