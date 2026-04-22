package com.bbmovie.search.service.seeder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorIndexOptionsType;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.bbmovie.search.entity.MovieDocument;
import com.bbmovie.search.service.embedding.DjLEmbeddingService;
import com.bbmovie.search.utils.EmbeddingUtils;
import com.bbmovie.search.utils.QdrantHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points.PointStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.VectorsFactory.vectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieSeederService {

    private final Optional<ElasticsearchClient> elasticsearchClientOptional;
    private final Optional<QdrantClient> qdrantClientOptional;
    private final Optional<EmbeddingModel> embeddingModel;
    private final Optional<DjLEmbeddingService> djlEmbeddingService;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    @Value("${app.search.engine}")
    private String searchEngine;

    private static final int SAMPLE_COUNT = 500;
    private static final int EMBEDDING_DIM = 384;

    private boolean seedingComplete = false;

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfEmpty() {
        if (isProdProfile())
            return;
        if (seedingComplete)
            return;

        try {
            // Elasticsearch Seeding
            if (searchEngine.equalsIgnoreCase("elasticsearch")) {
                seedElasticsearch();
            }

            // Qdrant Seeding
            if (searchEngine.equalsIgnoreCase("qdrant")) {
                seedQdrant();
            }
        } catch (Exception e) {
            log.error("Error during seeding: ", e);
        } finally {
            seedingComplete = true;
        }
    }

    private void seedElasticsearch() throws IOException {
        if (!indexExists()) {
            log.warn("ES Index '{}' not found — creating...", indexName);
            createIndex();
        }

        if (isIndexEmpty()) {
            log.info("ES Index '{}' is empty — inserting {} sample movies...", indexName, SAMPLE_COUNT);
            insertSamples().block();
        } else {
            log.info("ES Index '{}' already contains data — skipping seeding.", indexName);
        }
    }

    private void seedQdrant() {
        try {
            QdrantClient qdrantClient;
            if (qdrantClientOptional.isPresent()) {
                qdrantClient = qdrantClientOptional.get();
            } else {
                log.warn("Qdrant Client not available, skipping seeding...");
                return;
            }
            List<String> collections = qdrantClient.listCollectionsAsync().get();

            boolean collectionExists = collections != null && collections.contains(indexName);

            if (collectionExists) {
                log.info("Collection '{}' exists!", indexName);
            } else {
                log.warn("Collection '{}' NOT found.", indexName);
            }

            if (!collectionExists) {
                log.warn("Qdrant Collection '{}' not found — creating...", indexName);
                qdrantClient.createCollectionAsync(
                        indexName,
                        VectorParams.newBuilder()
                                .setSize(EMBEDDING_DIM)
                                .setDistance(Distance.Cosine)
                                .build())
                        .get();
                log.info("Qdrant Collection '{}' created.", indexName);
            }

            Long result = qdrantClient.countAsync(indexName).get();
            long count = result == null ? 0 : result;

            if (count == 0) {
                log.info("Qdrant Collection '{}' is empty — inserting sample movies...", indexName);
                List<MovieDocument> movies = generateMovies();

                // Batch insert to Qdrant
                int batchSize = 50;
                for (int i = 0; i < movies.size(); i += batchSize) {
                    List<MovieDocument> batch = movies.subList(i, Math.min(i + batchSize, movies.size()));
                    List<PointStruct> points = new ArrayList<>();

                    for (MovieDocument m : batch) {
                        points.add(toPointStruct(m));
                    }

                    qdrantClient.upsertAsync(indexName, points).get();
                    log.info("Seeded batch {} to Qdrant", i / batchSize + 1);
                }
                log.info("Qdrant seeding completed.");
            } else {
                log.info("Qdrant Collection '{}' already contains data — skipping seeding.", indexName);
            }

        } catch (Exception e) {
            log.error("Failed to seed Qdrant", e);
        }
    }

    private PointStruct toPointStruct(MovieDocument m) {
        Map<String, Object> rawMap = objectMapper.convertValue(m, new TypeReference<>() {});
        Map<String, JsonWithInt.Value> qdrantPayload = new HashMap<>();

        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            JsonWithInt.Value convertedValue = QdrantHelper.objectToValue(entry.getValue()); // Hàm helper bên dưới
            if (convertedValue != null) {
                qdrantPayload.put(entry.getKey(), convertedValue);
            }
        }

        return PointStruct.newBuilder()
                .setId(id(UUID.fromString(m.getId())))
                .setVectors(vectors(EmbeddingUtils.convertToFloatList(m.getEmbedding())))
                .putAllPayload(qdrantPayload)
                .build();
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
    }

    private boolean indexExists() throws IOException {
        ElasticsearchClient elasticsearchClient;
        if (elasticsearchClientOptional.isPresent()) {
            elasticsearchClient = elasticsearchClientOptional.get();
        } else {
            log.warn("Elasticsearch client not available. Skipping indexExists...");
            return true;
        }
        return elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
    }

    private boolean isIndexEmpty() throws IOException {
        ElasticsearchClient elasticsearchClient;
        if (elasticsearchClientOptional.isPresent()) {
            elasticsearchClient = elasticsearchClientOptional.get();
        } else {
            log.warn("Elasticsearch client not available. Skipping isIndexEmpty...");
            return true;
        }
        var response = elasticsearchClient.search(s -> s.index(indexName)
                .size(0)
                .query(q -> q
                        .matchAll(m -> m)),
                Void.class);
        long count = Optional.ofNullable(response.hits().total())
                .map(TotalHits::value)
                .orElse(0L);
        return count == 0;
    }

    private void createIndex() throws IOException {
        ElasticsearchClient elasticsearchClient;
        if (elasticsearchClientOptional.isPresent()) {
            elasticsearchClient = elasticsearchClientOptional.get();
        } else {
            log.warn("Elasticsearch client not available. Skipping createIndex...");
            return;
        }
        // If the index already exists, skip creation
        boolean exists = elasticsearchClient.indices()
                .exists(e -> e.index(indexName))
                .value();
        if (exists) {
            log.info("Index '{}' already exists — skipping creation.", indexName);
            return;
        }

        elasticsearchClient.indices().create(c -> c
                .index(indexName)
                .settings(s -> s
                        .numberOfShards("1")
                        .numberOfReplicas("0")
                        .analysis(a -> a) // safe stub
                )
                .mappings(m -> m
                        .properties("id", p -> p.keyword(k -> k))
                        .properties("title", p -> p.text(t -> t))
                        .properties("description", p -> p.text(t -> t))
                        .properties("genres", p -> p.keyword(k -> k))
                        .properties("actors", p -> p.keyword(k -> k))
                        .properties("directors", p -> p.keyword(k -> k))
                        .properties("poster", p -> p.keyword(k -> k))
                        .properties("releaseYear", p -> p.integer(i -> i))
                        .properties("releaseDate", p -> p.date(d -> d))
                        .properties("rating", p -> p.double_(d -> d))
                        .properties("country", p -> p.keyword(k -> k))
                        .properties("type", p -> p.keyword(k -> k))

                        // FIXED: correct ordering + ANN enabled
                        .properties("embedding", p -> p.denseVector(v -> v
                                .dims(EMBEDDING_DIM)
                                .similarity(DenseVectorSimilarity.Cosine) // MUST come before index(true)
                                .index(true) // enable ANN
                                .indexOptions(io -> io
                                        .type(DenseVectorIndexOptionsType.BbqDisk)
                                // .m(16) // Only used with HNSW
                                // .efConstruction(100) // Only used with HNSW
                                )))));
        log.info("Index '{}' created.", indexName);
    }

    private Mono<Void> insertSamples() {
        ElasticsearchClient elasticsearchClient;
        if (elasticsearchClientOptional.isPresent()) {
            elasticsearchClient = elasticsearchClientOptional.get();
        } else {
            log.warn("Elasticsearch client not available. Skipping insertSamples...");
            return Mono.empty();
        }

        List<MovieDocument> movies = generateMovies();
        int batchSize = 20;
        AtomicInteger inserted = new AtomicInteger();

        return Flux.fromIterable(movies)
                .buffer(batchSize)
                .concatMap(batch -> Mono.fromCallable(() -> {
                    BulkRequest.Builder br = createBatchRequest(batch);
                    BulkResponse bulk = elasticsearchClient.bulk(br.build());
                    if (bulk.errors()) {
                        log.warn("Bulk insert had errors.");
                    }
                    return batch.size();
                })
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnSuccess(count -> {
                            int total = inserted.addAndGet(count);
                            log.info("Inserted {}/{} movies...", total, movies.size());
                        }))
                .then();
    }

    private BulkRequest.Builder createBatchRequest(List<MovieDocument> batch) {
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (MovieDocument movie : batch) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(movie.getId())
                            .document(movie)));
        }
        return br;
    }

    private List<MovieDocument> generateMovies() {
        List<MovieDocument> docs = new ArrayList<>();
        String[] genres = { "Sci-Fi", "Drama", "Romance", "Action", "Thriller", "Comedy", "Fantasy", "Mystery" };
        String[] directors = { "Christopher Nolan", "Ridley Scott", "Denis Villeneuve", "James Cameron",
                "Patty Jenkins" };
        String[] actors = { "Tom Hanks", "Natalie Portman", "Ryan Gosling", "Emma Stone", "Matt Damon",
                "Scarlett Johansson" };
        String[] countries = { "United States", "United Kingdom", "Japan", "France", "South Korea", "Canada" };
        String[] types = { "movie", "series" };

        String[] titleTemplates = {
                "The {adjective} {noun}",
                "{adjective} of {place}",
                "Echoes of {noun}",
                "Rise of the {adjective} {noun}",
                "{place} Diaries"
        };
        String[] adjectives = { "Silent", "Hidden", "Eternal", "Neon", "Lost", "Crimson", "Forgotten", "Infinite" };
        String[] nouns = { "Dreams", "Empire", "Voyage", "Legacy", "Code", "Mind", "Horizon" };
        String[] places = { "Mars", "Tokyo", "Tomorrow", "Atlantis", "Eden", "The Stars" };

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        IntStream.range(0, SAMPLE_COUNT).forEach(i -> {
            String titleTemplate = titleTemplates[rnd.nextInt(titleTemplates.length)];
            String title = titleTemplate
                    .replace("{adjective}", adjectives[rnd.nextInt(adjectives.length)])
                    .replace("{noun}", nouns[rnd.nextInt(nouns.length)])
                    .replace("{place}", places[rnd.nextInt(places.length)]);

            String genre = genres[rnd.nextInt(genres.length)];
            List<String> cast = List.of(
                    actors[rnd.nextInt(actors.length)],
                    actors[rnd.nextInt(actors.length)]);
            List<String> dir = List.of(directors[rnd.nextInt(directors.length)]);
            String country = countries[rnd.nextInt(countries.length)];
            String type = types[rnd.nextInt(types.length)];

            String description = String.format(
                    "In this %s %s film, %s and %s explore the depths of %s — a story about %s, sacrifice, and discovery.",
                    country,
                    genre.toLowerCase(),
                    cast.get(0),
                    cast.get(1),
                    cast.get(0),
                    nouns[rnd.nextInt(nouns.length)]);

            int releaseYear = rnd.nextInt(1980, 2025);
            String poster = "https://picsum.photos/seed/" + i + "/300/400";

            Instant start = Instant.parse("2000-01-01T00:00:00Z");
            Instant end = Instant.parse("2025-01-01T00:00:00Z");
            Instant releaseDate = generateRandomInstant(start, end);

            double rating = Math.round(rnd.nextDouble(1.0, 5.0) * 10.0) / 10.0;

            float[] embedding = generateEmbedding(title + " " + description);

            docs.add(MovieDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .title(title)
                    .description(description)
                    .genres(List.of(genre))
                    .actors(cast)
                    .directors(dir)
                    .releaseYear(releaseYear)
                    .poster(poster)
                    .country(country)
                    .type(type)
                    .rating(rating)
                    .releaseDate(releaseDate)
                    .embedding(embedding)
                    .build());
        });

        return docs;
    }

    private float[] generateEmbedding(String text) {
        try {
            if (djlEmbeddingService.isPresent()) {
                return djlEmbeddingService.get().generateEmbedding(text).block();
            } else if (embeddingModel.isPresent()) {
                return embeddingModel.get().embed(text);
            } else {
                // fallback: random small floats (deterministic shape)
                float[] vec = new float[EMBEDDING_DIM];
                for (int i = 0; i < vec.length; i++) {
                    vec[i] = ThreadLocalRandom.current().nextFloat(-1f, 1f);
                }
                return vec;
            }
        } catch (Exception e) {
            log.warn("Embedding failed for '{}': {}", text, e.getMessage());
            float[] fallback = new float[EMBEDDING_DIM];
            Arrays.fill(fallback, 0.01f);
            return fallback;
        }
    }

    public static Instant generateRandomInstant(Instant startInclusive, Instant endExclusive) {
        long startMillis = startInclusive.toEpochMilli();
        long endMillis = endExclusive.toEpochMilli();
        long randomMillis = ThreadLocalRandom.current().nextLong(startMillis, endMillis);
        return Instant.ofEpochMilli(randomMillis);
    }
}
