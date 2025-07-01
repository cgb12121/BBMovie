package com.example.bbmovie.service.kafka;

import com.example.common.dtos.kafka.VideoMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VideoMetadataProducer {

    private final KafkaTemplate<String, VideoMetadata> kafkaTemplate;

    @Autowired
    public VideoMetadataProducer(@Qualifier("videoMetadataKafkaTemplate") KafkaTemplate<String, VideoMetadata> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(VideoMetadata dto) {
        kafkaTemplate.send("movie.video.metadata.sync", dto.getMovieId().toString(), dto);
        log.info("Sent video metadata for movieId={}", dto.getMovieId());
    }
}
