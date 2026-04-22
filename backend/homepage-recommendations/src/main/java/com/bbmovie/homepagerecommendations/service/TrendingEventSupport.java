package com.bbmovie.homepagerecommendations.service;

import com.bbmovie.homepagerecommendations.config.HomepageRecommendationsProperties;
import com.bbmovie.homepagerecommendations.dto.PlaybackAnalyticsEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

final class TrendingEventSupport {

    private TrendingEventSupport() {
    }

    static ZoneId zone(HomepageRecommendationsProperties properties) {
        return ZoneId.of(properties.getTrending().getZoneId());
    }

    static LocalDate dayForEvent(PlaybackAnalyticsEvent event, ZoneId zoneId) {
        long ts = event != null && event.timestampEpochSec() > 0
                ? event.timestampEpochSec()
                : Instant.now().getEpochSecond();
        return Instant.ofEpochSecond(ts).atZone(zoneId).toLocalDate();
    }

    static double deltaFor(PlaybackAnalyticsEvent event, HomepageRecommendationsProperties properties) {
        if (event == null) {
            return 0.0;
        }
        return event.completed()
                ? properties.getTrending().getCompletedDelta()
                : properties.getTrending().getPartialDelta();
    }

    static String dailyZsetKey(LocalDate day, HomepageRecommendationsProperties properties) {
        return properties.getTrending().getDailyKeyPrefix() + ":" + day;
    }

    static String bufferCompositeKey(LocalDate day, UUID movieId) {
        return day.toString() + "|" + movieId;
    }
}
