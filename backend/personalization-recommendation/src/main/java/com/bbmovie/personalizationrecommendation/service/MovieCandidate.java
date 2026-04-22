package com.bbmovie.personalizationrecommendation.service;

import java.util.List;
import java.util.UUID;

public record MovieCandidate(
        UUID movieId,
        double vectorScore,
        List<String> genres,
        double popularity,
        double freshness
) {
}

