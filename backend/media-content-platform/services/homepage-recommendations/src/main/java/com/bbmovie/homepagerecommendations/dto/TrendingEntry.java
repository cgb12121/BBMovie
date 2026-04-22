package com.bbmovie.homepagerecommendations.dto;

import java.util.UUID;

public record TrendingEntry(UUID movieId, double score) {
}
