package com.bbmovie.mediaservice.dto;

import com.bbmovie.mediaservice.entity.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {
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
    private String fileId; // Link to the uploaded file
    private MovieStatus status;
}