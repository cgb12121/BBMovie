package com.bbmovie.auth.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CountryUniversityResponse(
    String country,
    long total,
    List<UniversitySummary> universities,
    boolean hasMore
) {}