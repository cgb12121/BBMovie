package com.bbmovie.mediaservice.dto;

import com.bbmovie.mediaservice.entity.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {
    private UUID id;
    private UUID movieId;
    private String title;
    private String description;
    private String director;
    private String cast;
    private Integer duration; // in minutes
    private String genre;
    private String releaseDate;
    private String posterUrl;
    private String trailerUrl;
    private String fileId;
    private MovieStatus status;
    private String filePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}