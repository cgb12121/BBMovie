package com.example.bbmoviestream.service;

import com.example.bbmoviestream.entity.VideoMetadata;
import com.example.bbmoviestream.repository.VideoMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VideoMetadataConsumer {

    private final VideoMetadataRepository repository;

    @Autowired
    public VideoMetadataConsumer(VideoMetadataRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "movie.video.metadata.sync", groupId = "video-stream-service")
    public void consume(com.example.bbmoviestream.dto.VideoMetadata dto) {
        log.info("Received metadata for movieId={}", dto.getMovieId());
        repository.save(VideoMetadata.builder()
                .movieId(dto.getMovieId())
                .title(dto.getTitle())
                .videoUrl(dto.getVideoUrl())
                .videoPublicId(dto.getVideoPublicId())
                .trailerUrl(dto.getTrailerUrl())
                .trailerPublicId(dto.getTrailerPublicId())
                .videoQuality(dto.getVideoQuality())
                .durationMinutes(dto.getDurationMinutes())
                .posterUrl(dto.getPosterUrl())
                .posterPublicId(dto.getPosterPublicId())
                .isActive(dto.getIsActive())
                .build());
    }

    @KafkaListener(topics = "movie.video.metadata.sync.dlt", groupId = "video-stream-service")
    public void handleDeadLetter(VideoMetadata dto) {
        log.warn("DLT received for movieId={} — please inspect data!", dto.getMovieId());
    }
}
