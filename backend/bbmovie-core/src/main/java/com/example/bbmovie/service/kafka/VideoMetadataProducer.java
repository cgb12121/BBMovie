package com.example.bbmovie.service.kafka;

import com.example.bbmovie.dto.kafka.publisher.VideoMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoMetadataProducer {

    private final KafkaTemplate<String, VideoMetadata> kafkaTemplate;

    public void send(VideoMetadata dto) {
        kafkaTemplate.send("movie.video.metadata.sync", dto.getMovieId().toString(), dto);
        log.info("Sent video metadata for movieId={}", dto.getMovieId());
    }
}
