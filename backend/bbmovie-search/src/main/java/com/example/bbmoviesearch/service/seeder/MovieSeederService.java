package com.example.bbmoviesearch.service.seeder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.bbmoviesearch.entity.MovieDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieSeederService {

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingModel embeddingModel;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    private static final int SAMPLE_COUNT = 100;

    @EventListener
    public void seedIfEmpty(ApplicationReadyEvent event) {
        try {
            if (!indexExists()) {
                log.warn("Index '{}' not found — creating it...", indexName);
                createIndex();
            }

            if (isIndexEmpty()) {
                log.info("Index '{}' is empty — inserting {} sample movies...", indexName, SAMPLE_COUNT);
                insertSamples();
                log.info("Seeding complete!");
            } else {
                log.info("Index '{}' already has data — skipping seeding.", indexName);
            }
        } catch (Exception e) {
            log.error("Error during seeding: ", e);
        }
    }

    private boolean indexExists() throws IOException {
        return elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
    }

    private void createIndex() throws Exception {
        elasticsearchClient.indices().create(c -> c
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
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                .index(indexName)
                .size(1)
                .query(q -> q.matchAll(m -> m)), Void.class);

        long count = response.hits().total() != null ? response.hits().total().value() : 0;
        return count == 0;
    }

    private void insertSamples() throws Exception {
        List<MovieDocument> movies = generateSampleMovies();

        BulkRequest.Builder br = new BulkRequest.Builder();
        for (MovieDocument movie : movies) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(movie.getId())
                            .document(movie)
                    )
            );
        }

        BulkResponse bulkResponse = elasticsearchClient.bulk(br.build());
        if (bulkResponse.errors()) {
            log.warn("Some bulk operations failed: {}", bulkResponse.items());
        }
    }

    private List<MovieDocument> generateSampleMovies() {
        String[] sampleCategories = {"Action", "Drama", "Comedy", "Sci-Fi", "Romance", "Thriller", "Fantasy"};
        List<MovieDocument> list = new ArrayList<>();

        IntStream.range(0, SAMPLE_COUNT).forEach(i -> {
            String title = "Sample Movie " + (i + 1);
            String description = "A thrilling adventure of movie number " + (i + 1);
            List<String> categories = List.of(
                    sampleCategories[ThreadLocalRandom.current().nextInt(sampleCategories.length)]
            );
            double rating = ThreadLocalRandom.current().nextDouble(1, 5);
            String posterUrl = "https://picsum.photos/seed/" + i + "/200/300";
            LocalDateTime releaseDate = LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(0, 2000));

            // Create embedding using Ollama/DJL model
            float[] embedding = embeddingModel.embed(title + " " + description);

            list.add(MovieDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .title(title)
                    .description(description)
                    .embedding(embedding)
                    .rating(rating)
                    .categories(categories)
                    .posterUrl(posterUrl)
                    .releaseDate(releaseDate)
                    .build());
        });

        return list;
    }
}
