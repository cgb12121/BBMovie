package com.bbmovie.common.dtos.nats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoMetadata {
    private Long movieId;
    private String title;
    private String description;
    private List<String> categories;
    private String country;
    private String movieType;
    private Instant releaseDate;
    private String videoUrl;
    private String videoPublicId;
    private String trailerUrl;
    private String trailerPublicId;
    private Set<String> videoQuality;
    private Integer durationMinutes;
    private String posterUrl;
    private String posterPublicId;
    private Boolean isActive;
}