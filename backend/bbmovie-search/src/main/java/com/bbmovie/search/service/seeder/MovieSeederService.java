package com.bbmovie.search.service.seeder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.bbmovie.search.dto.event.ElasticsearchUpEvent;
import com.bbmovie.search.entity.MovieDocument;
import com.bbmovie.search.service.embedding.DjLEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Service
public class MovieSeederService {

    private final ElasticsearchClient elasticsearchClient;
    private final Optional<EmbeddingModel> embeddingModel;
    private final Optional<DjLEmbeddingService> djlEmbeddingService;
    private final Environment environment;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;
    private boolean seedingComplete = false;
    private static final int SAMPLE_COUNT = 1000;

    @Autowired
    public MovieSeederService(
            ElasticsearchClient elasticsearchClient,
            Optional<EmbeddingModel> embeddingModel,
            Optional<DjLEmbeddingService> djlEmbeddingService,
            Environment environment) {
        this.elasticsearchClient = elasticsearchClient;
        this.embeddingModel = embeddingModel;
        this.djlEmbeddingService = djlEmbeddingService;
        this.environment = environment;
    }

    @EventListener({ ApplicationReadyEvent.class, ElasticsearchUpEvent.class })
    public void seedIfEmpty() {
        String[] activeProfiles = environment.getActiveProfiles();

        for (String profile : activeProfiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                return;
            }
        }

        if (seedingComplete) {
            return;
        }

        try {
            if (!indexExists()) {
                log.warn("Index '{}' not found — creating it...", indexName);
                createIndex();
            }

            if (isIndexEmpty()) {
                log.info("Index '{}' is empty — inserting {} sample movies...", indexName, SAMPLE_COUNT);
                insertSamples()
                        .doOnSuccess(v -> log.info("Seeding complete!"))
                        .doOnError(e -> log.error("Error during reactive seeding: ", e))
                        .block();
            } else {
                log.info("Index '{}' already has data — skipping seeding.", indexName);
            }
        } catch (Exception e) {
            log.error("Error during seeding setup: ", e);
        } finally {
            seedingComplete = true;
        }
    }

    private boolean indexExists() throws IOException {
        return elasticsearchClient
                .indices()
                .exists(e -> e.index(indexName))
                .value();
    }

    //Only use when testing/developing
    @SuppressWarnings("unused")
    private void deleteIndex() throws Exception {
        if (indexExists()) {
            elasticsearchClient
                    .indices()
                    .delete(d -> d.index(indexName));
            log.info("Index '{}' deleted successfully.", indexName);
        } else {
            log.info("Index '{}' does not exist, skipping deletion.", indexName);
        }
    }

    private void createIndex() throws Exception {
        elasticsearchClient
                .indices()
                .create(c -> c
                    .index(indexName)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("0")
                    )
                    .mappings(m -> m
                            .properties("id", p -> p.keyword(k -> k))
                            .properties("title", p -> p.text(t -> t.analyzer("standard")))
                            .properties("description", p -> p.text(t -> t.analyzer("standard")))
                            .properties("categories", p -> p.keyword(k -> k))
                            .properties("posterUrl", p -> p.keyword(k -> k))
                            .properties("type", p -> p.keyword(k -> k))
                            .properties("rating", p -> p.double_(d -> d))
                            .properties("releaseDate", p -> p.date(d -> d))
                            .properties("embedding", p -> p.denseVector(v -> v
                                    .dims(384)
                                    .index(true)
                                    .similarity("cosine")
                                    .indexOptions(io -> io
                                            .type("int8_hnsw")
                                            .m(16)
                                            .efConstruction(100)
                                    )
                            ))
                    )
                );
        log.info("Index '{}' created successfully.", indexName);
    }

    private boolean isIndexEmpty() throws IOException {
        SearchResponse<Void> response = elasticsearchClient
                .search(s -> s
                    .index(indexName)
                    .size(0)
                    .query(q -> q.matchAll(m -> m))
                , Void.class);

        long count = response.hits().total() != null
                ? response.hits().total().value()
                : 0;
        return count == 0;
    }

    private Mono<Void> insertSamples() {
        List<MovieDocument> movies = generateSampleMovies();
        int batchSize = 20; // Process 20 movies at a time
        int totalMovies = movies.size();
        AtomicInteger insertedCount = new AtomicInteger(0);

        return Flux.fromIterable(movies)
                .buffer(batchSize)
                .concatMap(batch ->
                        Mono.fromCallable(() -> {
                                    BulkRequest.Builder br = buildBulkRequest(batch);
                                    BulkResponse bulkResponse = elasticsearchClient.bulk(br.build());
                                    if (bulkResponse.errors()) {
                                        log.warn("Some bulk operations failed in this batch.");
                                    }
                                    return batch.size();
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnSuccess(count -> {
                                    int currentTotal = insertedCount.addAndGet(count);
                                    log.info("Inserted {}/{} movies...", currentTotal, totalMovies);
                                })
                )
                .then();
    }

    private BulkRequest.Builder buildBulkRequest(List<MovieDocument> batch) {
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (MovieDocument movie : batch) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(movie.getId())
                            .document(movie)
                    )
            );
        }
        return br;
    }

    private List<MovieDocument> generateSampleMovies() {
        String[] sampleCategories = {"Action", "Drama", "Comedy", "Sci-Fi", "Romance", "Thriller", "Fantasy"};
        String[] movieTypes = {"movie", "series"};
        List<MovieDocument> list = new ArrayList<>();

        IntStream.range(0, SAMPLE_COUNT).forEach(i -> {
            String title = "Sample Movie " + (i + 1);
            String description = "A thrilling adventure of movie number " + (i + 1);
            List<String> categories = List.of(
                    sampleCategories[ThreadLocalRandom.current().nextInt(sampleCategories.length)]
            );
            String types = movieTypes[ThreadLocalRandom.current().nextInt(movieTypes.length)];

            double rating = ThreadLocalRandom.current().nextDouble(1, 5);
            String posterUrl = "https://picsum.photos/seed/" + i + "/200/300";
            LocalDateTime releaseDate = LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(0, 2000));

            // Create embedding
            float[] embedding = new float[384]; // Default to empty embedding

            if (djlEmbeddingService.isPresent()) {
                embedding = djlEmbeddingService.get().generateEmbedding(title + " " + description).block();
                log.debug("Generated embedding using DJL for movie: {}", title);
            } else if (embeddingModel.isPresent()) {
                embedding = embeddingModel.get().embed(title + " " + description);
                log.debug("Generated embedding using Spring AI EmbeddingModel for movie: {}", title);
            } else {
                log.warn("No embedding provider available. Using empty embedding for movie: {}", title);
            }

            list.add(MovieDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .title(title)
                    .description(description)
                    .embedding(embedding)
                    .type(types)
                    .rating(rating)
                    .categories(categories)
                    .posterUrl(posterUrl)
                    .releaseDate(releaseDate)
                    .build());
        });

        return list;
    }
}
