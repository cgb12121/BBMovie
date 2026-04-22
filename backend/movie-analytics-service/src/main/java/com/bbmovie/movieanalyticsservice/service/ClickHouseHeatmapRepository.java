package com.bbmovie.movieanalyticsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnBean(name = "clickHouseJdbcTemplate")
public class ClickHouseHeatmapRepository {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public void appendEvent(HeatmapIngestEvent event) {
        try {
            for (Map.Entry<Integer, Integer> e : event.segmentCounts().entrySet()) {
                clickHouseJdbcTemplate.update(
                        "INSERT INTO movie_heatmap_raw (movie_id, bucket_size, bucket_index, event_count, occurred_at) VALUES (?,?,?,?,?)",
                        event.movieId().toString(),
                        event.bucketSize(),
                        e.getKey(),
                        e.getValue(),
                        Timestamp.from(event.occurredAt() == null ? Instant.now() : event.occurredAt())
                );
            }
        } catch (Exception ex) {
            log.warn("ClickHouse append failed: {}", ex.getMessage());
        }
    }

    public Map<Integer, Long> loadAggregated(UUID movieId, int bucketSize, int maxBuckets) {
        Map<Integer, Long> buckets = new LinkedHashMap<>();
        try {
            clickHouseJdbcTemplate.queryForList(
                            """
                            SELECT bucket_index, sum(event_count) AS total
                            FROM movie_heatmap_raw
                            WHERE movie_id = ? AND bucket_size = ?
                            GROUP BY bucket_index
                            ORDER BY bucket_index ASC
                            LIMIT ?
                            """,
                            movieId.toString(),
                            bucketSize,
                            maxBuckets
                    )
                    .forEach(row -> buckets.put(
                            ((Number) row.get("bucket_index")).intValue(),
                            ((Number) row.get("total")).longValue()
                    ));
        } catch (Exception ex) {
            log.warn("ClickHouse aggregate query failed: {}", ex.getMessage());
        }
        return buckets;
    }
}

