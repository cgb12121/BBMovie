package com.bbmovie.watchhistory.service;

import com.bbmovie.watchhistory.config.WatchTrackingProperties;
import com.bbmovie.watchhistory.dto.PlaybackAnalyticsEvent;
import com.bbmovie.watchhistory.dto.PlaybackTrackRequest;
import com.bbmovie.watchhistory.dto.ResumeListPageResponse;
import com.bbmovie.watchhistory.dto.ResumeResponse;
import com.bbmovie.watchhistory.dto.TrackPlaybackResponse;
import com.bbmovie.watchhistory.model.ResumeState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchHistoryTrackingService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final WatchTrackingProperties properties;
    private final NatsPlaybackPublisher natsPlaybackPublisher;

    public TrackPlaybackResponse track(String userId, PlaybackTrackRequest request) {
        int segDur = Math.max(1, properties.getSegmentDurationSec());
        double pos = request.getPositionSec();
        double dur = request.getDurationSec() != null ? request.getDurationSec() : 0;

        int segmentIndex = (int) Math.floor(pos / segDur);
        boolean completedNow = resolveCompleted(request, dur);

        String posKey = String.format(properties.getRedis().getPosKeyPattern(), userId);
        String field = request.getMovieId().toString();
        String segKey = String.format(properties.getRedis().getSegmentKeyPattern(), userId, request.getMovieId());

        ResumeState previous = readResume(posKey, field);
        String lastSegRaw = redis.opsForValue().get(segKey);
        Integer lastPublishedSeg = lastSegRaw == null ? null : Integer.valueOf(lastSegRaw);

        long nowSec = Instant.now().getEpochSecond();
        ResumeState newState = new ResumeState(pos, dur, nowSec, completedNow);
        try {
            redis.opsForHash().put(posKey, field, objectMapper.writeValueAsString(newState));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize resume state", e);
        }
        redis.expire(posKey, Duration.ofDays(properties.getRedis().getPosTtlDays()));

        boolean shouldPublish =
                shouldPublishAnalytics(lastPublishedSeg, segmentIndex, previous, completedNow);

        boolean analyticsSent = false;
        if (shouldPublish) {
            PlaybackAnalyticsEvent event = new PlaybackAnalyticsEvent(
                    UUID.randomUUID().toString(),
                    userId,
                    request.getMovieId(),
                    segmentIndex,
                    pos,
                    nowSec,
                    completedNow,
                    request.getMetadata());
            analyticsSent = natsPlaybackPublisher.tryPublish(event);
            if (analyticsSent || !natsPlaybackPublisher.isNatsEnabled()) {
                redis.opsForValue().set(
                        segKey,
                        String.valueOf(segmentIndex),
                        Duration.ofHours(properties.getRedis().getSegmentTtlHours()));
            }
        }

        int flushHint = Math.max(5, properties.getSuggestedClientFlushSec());
        long nextTrackAt = nowSec + flushHint;
        return TrackPlaybackResponse.builder()
                .status("accepted")
                .resumeCached(true)
                .analyticsEventPublished(analyticsSent)
                .nextTrackAtEpochSec(nextTrackAt)
                .build();
    }

    public Optional<ResumeResponse> getResume(String userId, UUID movieId) {
        String posKey = String.format(properties.getRedis().getPosKeyPattern(), userId);
        ResumeState state = readResume(posKey, movieId.toString());
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(ResumeResponse.builder()
                .movieId(movieId)
                .positionSec(state.positionSec())
                .durationSec(state.durationSec())
                .updatedAtEpochSec(state.updatedAtEpochSec())
                .completed(state.completed())
                .build());
    }

    public ResumeListPageResponse listResumeStatesPage(String userId, String cursor, int limit) {
        String posKey = String.format(properties.getRedis().getPosKeyPattern(), userId);
        int count = Math.min(Math.max(limit, 1), 200);
        String cursorIn = cursor == null || cursor.isBlank() ? "0" : cursor;

        ResumeListPageResponse page = redis.execute((RedisCallback<ResumeListPageResponse>) connection -> {
            Object raw = connection.execute(
                    "HSCAN",
                    posKey.getBytes(StandardCharsets.UTF_8),
                    cursorIn.getBytes(StandardCharsets.UTF_8),
                    "COUNT".getBytes(StandardCharsets.US_ASCII),
                    String.valueOf(count).getBytes(StandardCharsets.US_ASCII));
            if (!(raw instanceof List<?> top) || top.size() < 2) {
                return new ResumeListPageResponse(List.of(), "0");
            }
            String next = toUtf8String(top.get(0));
            Object entriesObj = top.get(1);
            List<ResumeResponse> items = new ArrayList<>();
            if (entriesObj instanceof List<?> entryList) {
                for (int i = 0; i + 1 < entryList.size(); i += 2) {
                    UUID movieId;
                    try {
                        movieId = UUID.fromString(toUtf8String(entryList.get(i)));
                    } catch (IllegalArgumentException ex) {
                        continue;
                    }
                    String json = toUtf8String(entryList.get(i + 1));
                    ResumeState state;
                    try {
                        state = objectMapper.readValue(json, ResumeState.class);
                    } catch (Exception e) {
                        log.warn("Corrupt resume state in HSCAN for {}: {}", movieId, e.getMessage());
                        continue;
                    }
                    items.add(ResumeResponse.builder()
                            .movieId(movieId)
                            .positionSec(state.positionSec())
                            .durationSec(state.durationSec())
                            .updatedAtEpochSec(state.updatedAtEpochSec())
                            .completed(state.completed())
                            .build());
                }
            }
            items.sort(Comparator.comparingLong(ResumeResponse::getUpdatedAtEpochSec).reversed());
            String nextCursor = next == null || next.isEmpty() ? "0" : next;
            return new ResumeListPageResponse(items, nextCursor);
        });
        return page == null ? new ResumeListPageResponse(List.of(), "0") : page;
    }

    private static String toUtf8String(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof byte[] b) {
            return new String(b, StandardCharsets.UTF_8);
        }
        if (o instanceof ByteBuffer bb) {
            byte[] arr = new byte[bb.remaining()];
            bb.get(arr);
            return new String(arr, StandardCharsets.UTF_8);
        }
        return Objects.toString(o, "");
    }

    private static boolean resolveCompleted(PlaybackTrackRequest request, double durationSec) {
        if (Boolean.TRUE.equals(request.getCompleted())) {
            return true;
        }
        if (durationSec > 0 && request.getPositionSec() != null) {
            return request.getPositionSec() >= durationSec - 0.5;
        }
        return false;
    }

    private static boolean shouldPublishAnalytics(
            Integer lastPublishedSeg, int segmentIndex, ResumeState previous, boolean completedNow) {
        boolean segmentIsNew = lastPublishedSeg == null || lastPublishedSeg != segmentIndex;
        boolean completionEdge = completedNow && (previous == null || !previous.completed());
        return segmentIsNew || completionEdge;
    }

    private ResumeState readResume(String posKey, String field) {
        Object raw = redis.opsForHash().get(posKey, field);
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.readValue(raw.toString(), ResumeState.class);
        } catch (Exception e) {
            log.warn("Corrupt resume state for {} field {}: {}", posKey, field, e.getMessage());
            return null;
        }
    }
}
