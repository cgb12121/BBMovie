package com.example.bbmoviestream.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.Set;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadata {
    @Id
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