package com.bbmovie.search.service.nats;

import com.bbmovie.search.config.NatsConfig;
import com.bbmovie.search.dto.event.NatsConnectionEvent;
import com.bbmovie.search.entity.MovieDocument;
import com.bbmovie.search.repository.esclient.MovieMetadataRepository;
import com.bbmovie.search.service.embedding.EmbeddingService;
import com.example.common.dtos.nats.VideoMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Dispatcher;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Log4j2
@Service
public class VideoEventConsumer {

    private final Connection nats;
    private final ObjectMapper objectMapper;
    private final MovieMetadataRepository movieMetadataRepository;
    private final EmbeddingService embeddingService;

    @Autowired
    public VideoEventConsumer(
            NatsConfig.NatsConnectionFactory natsConnectionFactory,
            ObjectMapper objectMapper,
            MovieMetadataRepository movieMetadataRepository,
            EmbeddingService embeddingService) {
        this.nats = natsConnectionFactory.getConnection();
        this.objectMapper = objectMapper;
        this.movieMetadataRepository = movieMetadataRepository;
        this.embeddingService = embeddingService;
    }

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected/reconnected, (re)subscribing…");
            setupVideoEventSubscriptions();
        }
    }

    private void setupVideoEventSubscriptions() {
        Dispatcher dispatcher = this.nats.createDispatcher(msg -> {
            try {
                VideoMetadata videoMetadata = objectMapper.readValue(msg.getData(), VideoMetadata.class);
                handle(videoMetadata)
                        .doOnSuccess(v -> msg.ack())
                        .doOnError(e -> {
                            log.error("Error processing video metadata event. Message will be redelivered.", e);
                            msg.nak();
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe();
            } catch (Exception e) {
                log.error("Failed to deserialize video metadata event. Terminating message.", e);
                msg.term(); // Terminate poison pill messages
            }
        });

        dispatcher.subscribe("video.metadata");
        log.info("Subscribed to video.metadata events");
    }

    private Mono<Void> handle(VideoMetadata videoMetadata) {
        log.info("Received video metadata event for movieId: {}", videoMetadata.getMovieId());

        return movieMetadataRepository.findById(String.valueOf(videoMetadata.getMovieId()))
                .flatMap(movieDocument -> {
                    log.info("Found movie document with id {}, updating posterUrl.", movieDocument.getId());
                    movieDocument.setPosterUrl(videoMetadata.getPosterUrl());
                    return movieMetadataRepository.save(movieDocument);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("MovieDocument with id {} not found. Creating a new one.", videoMetadata.getMovieId());

                    Mono<float[]> embeddingMono = embeddingService.generateEmbedding((videoMetadata.getTitle() + videoMetadata.getDescription()));

                    return embeddingMono.map(embedding ->
                            MovieDocument.builder()
                                    .id(String.valueOf(videoMetadata.getMovieId()))
                                    .title(videoMetadata.getTitle())
                                    .description(videoMetadata.getDescription())
                                    .categories(videoMetadata.getCategories())
                                    .country(videoMetadata.getCountry())
                                    .type(videoMetadata.getMovieType())
                                    .posterUrl(videoMetadata.getPosterUrl())
                                    .releaseDate(videoMetadata.getReleaseDate())
                                    .embedding(embedding)
                                    .build()
                    ).flatMap(movieMetadataRepository::save);
                }))
                .doOnSuccess(savedDoc -> log.info("Successfully upserted document for movieId: {}", savedDoc.getId()))
                .doOnError(e -> log.error("Failed to upsert document for movieId: {}", videoMetadata.getMovieId(), e))
                .then(); // Convert to Mono<Void>
    }
}