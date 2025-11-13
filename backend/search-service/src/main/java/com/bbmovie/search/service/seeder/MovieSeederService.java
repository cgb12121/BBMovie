package com.bbmovie.search.service.seeder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
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
import java.time.Instant;
import java.util.*;
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

    private static final int SAMPLE_COUNT = 500;
    private static final int EMBEDDING_DIM = 384;

    private boolean seedingComplete = false;

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

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfEmpty() {
        if (isProdProfile()) return;
        if (seedingComplete) return;

        try {
            if (!indexExists()) {
                log.warn("Index '{}' not found — creating...", indexName);
                createIndex();
            }

            if (isIndexEmpty()) {
                log.info("Index '{}' is empty — inserting {} sample movies...", indexName, SAMPLE_COUNT);
                insertSamples().block();
            } else {
                log.info("Index '{}' already contains data — skipping seeding.", indexName);
            }

        } catch (Exception e) {
            log.error("Error during seeding: ", e);
        } finally {
            seedingComplete = true;
        }
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
    }

    private boolean indexExists() throws IOException {
        return elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
    }

    private boolean isIndexEmpty() throws IOException {
        var response = elasticsearchClient.search(s -> s.index(indexName)
                .size(0)
                .query(q -> q
                        .matchAll(m -> m)
                ), Void.class
        );
        long count = Optional.ofNullable(response.hits().total())
                .map(TotalHits::value)
                .orElse(0L);
        return count == 0;
    }

    private void createIndex() throws IOException {
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
                .settings(s -> s.numberOfShards("1").numberOfReplicas("0"))
                .mappings(m -> m
                        .properties("id", p -> p.keyword(k -> k))
                        .properties("title", p -> p.text(t -> t.analyzer("standard")))
                        .properties("description", p -> p.text(t -> t.analyzer("standard")))
                        .properties("genres", p -> p.keyword(k -> k))
                        .properties("actors", p -> p.keyword(k -> k))
                        .properties("directors", p -> p.keyword(k -> k))
                        .properties("poster", p -> p.keyword(k -> k))
                        .properties("releaseYear", p -> p.integer(i -> i))
                        .properties("releaseDate", p -> p.date(d -> d))
                        .properties("rating", p -> p.double_(d -> d))
                        .properties("country", p -> p.keyword(k -> k))
                        .properties("type", p -> p.keyword(k -> k))
                        .properties("embedding", p -> p.denseVector(v -> v
                                .dims(EMBEDDING_DIM)
                                .index(true)
                                .similarity("cosine")
                                .indexOptions(io -> io
                                        .type("hnsw")
                                        .m(16)
                                        .efConstruction(100)
                                )))

                        // This will be used for rag audit at AI Assistant Service
//                        .properties("audit", p -> p.object(o -> o
//                                .properties("latency_ms", pp -> pp.long_(l -> l))
//                                .properties("prompt_tokens", pp -> pp.integer(i -> i))
//                                .properties("response_tokens", pp -> pp.integer(i -> i))
//                                .properties("interaction_type", pp -> pp.keyword(k -> k))
//                                .properties("timestamp", pp -> pp.date(d -> d))
//                        ))
                )
        );
        log.info("Index '{}' created.", indexName);
    }

    private Mono<Void> insertSamples() {
        List<MovieDocument> movies = generateMovies();
        int batchSize = 20;
        AtomicInteger inserted = new AtomicInteger();

        return Flux.fromIterable(movies)
                .buffer(batchSize)
                .concatMap(batch ->
                        Mono.fromCallable(() -> {
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
                                })
                )
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
        String[] genres = {"Sci-Fi", "Drama", "Romance", "Action", "Thriller", "Comedy", "Fantasy", "Mystery"};
        String[] directors = {"Christopher Nolan", "Ridley Scott", "Denis Villeneuve", "James Cameron", "Patty Jenkins"};
        String[] actors = {"Tom Hanks", "Natalie Portman", "Ryan Gosling", "Emma Stone", "Matt Damon", "Scarlett Johansson"};
        String[] countries = {"United States", "United Kingdom", "Japan", "France", "South Korea", "Canada"};
        String[] types = {"movie", "series"};

        String[] titleTemplates = {
                "The {adjective} {noun}",
                "{adjective} of {place}",
                "Echoes of {noun}",
                "Rise of the {adjective} {noun}",
                "{place} Diaries"
        };
        String[] adjectives = {"Silent", "Hidden", "Eternal", "Neon", "Lost", "Crimson", "Forgotten", "Infinite"};
        String[] nouns = {"Dreams", "Empire", "Voyage", "Legacy", "Code", "Mind", "Horizon"};
        String[] places = {"Mars", "Tokyo", "Tomorrow", "Atlantis", "Eden", "The Stars"};

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
                    actors[rnd.nextInt(actors.length)]
            );
            List<String> dir = List.of(directors[rnd.nextInt(directors.length)]);
            String country = countries[rnd.nextInt(countries.length)];
            String type = types[rnd.nextInt(types.length)];

            String description = String.format(
                    "In this %s %s film, %s and %s explore the depths of %s — a story about %s, sacrifice, and discovery.",
                    country,
                    genre.toLowerCase(),
                    cast.get(0),
                    cast.get(1),
                    places[rnd.nextInt(places.length)],
                    nouns[rnd.nextInt(nouns.length)]
            );

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

