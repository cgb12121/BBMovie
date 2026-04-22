package com.bbmovie.personalizationrecommendation.service;

import java.util.List;
import java.util.UUID;

public interface CandidateProvider {
    List<MovieCandidate> candidates(UUID userId, List<Double> profileVector, int limit);
}

