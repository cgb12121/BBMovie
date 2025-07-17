package com.example.bbmoviestream.service.kafka;

import com.example.bbmoviestream.entity.Movie;
import com.example.bbmoviestream.repository.MovieRepository;
import com.example.common.dtos.kafka.VideoMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VideoMetadataConsumer {

    private final MovieRepository repository;

    @Autowired
    public VideoMetadataConsumer(MovieRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "movie.video.metadata.sync", groupId = "video-stream-service")
    public void consume(com.example.common.dtos.kafka.VideoMetadata dto) {
        log.info("Received metadata for movieId={}", dto.getMovieId());
        Movie newMovie = Movie.builder()
                .id(String.valueOf(dto.getMovieId()))
                .title(dto.getTitle())
                .filename(dto.getTitle())
                .filePath(dto.getVideoUrl())
                .build();
        repository.save(newMovie).subscribe();
    }

    @KafkaListener(topics = "movie.video.metadata.sync.dlt", groupId = "video-stream-service")
    public void handleDeadLetter(VideoMetadata dto) {
        log.warn("DLT received for movieId={} — please inspect data!", dto.getMovieId());
    }
}
