package com.bbmovie.watchhistory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlaybackAnalyticsEvent(
        String eventId,
        String userId,
        UUID movieId,
        int segmentIndex,
        double positionSec,
        long timestampEpochSec,
        boolean completed,
        Map<String, Object> metadata) {
}
