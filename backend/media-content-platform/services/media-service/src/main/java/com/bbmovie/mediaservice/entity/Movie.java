package com.bbmovie.mediaservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "movies")
@EntityListeners(AuditingEntityListener.class)
public class Movie {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID movieId; // This is the external ID used by other services

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String director;

    private String cast;

    private Integer duration; // in minutes

    private String genre;

    private String releaseDate;

    private String posterUrl;

    private String trailerUrl;

    // This is the ID from media-upload-service
    private String fileId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MovieStatus status = MovieStatus.DRAFT;

    private String filePath; // Path to the processed file in storage
    
    @Column(length = 2000)
    private String availableResolutions; // JSON array string, e.g. ["360p","720p","1080p"]

    private String maxAvailableResolution; // e.g. 1080p, 2160p

    private Integer sourceWidth; // source video width from probe/transcode

    private Integer sourceHeight; // source video height from probe/transcode

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}