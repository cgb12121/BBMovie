package com.bbmovie.personalizationrecommendation.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnMissingBean(QdrantCandidateProvider.class)
public class FallbackCandidateProvider implements CandidateProvider {
    @Override
    public List<MovieCandidate> candidates(UUID userId, List<Double> profileVector, int limit) {
        return List.of();
    }
}

