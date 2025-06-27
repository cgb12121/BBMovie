package com.example.bbmovie.dto.kafka.publisher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoMetadata {
    private Long movieId;
    private String title;
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
