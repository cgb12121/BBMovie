package com.bbmovie.personalizationrecommendation.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface UserFeatureRepository {
    Map<String, Double> readGenreAffinity(UUID userId);

    Set<UUID> readSeenMovieIds(UUID userId);
}

