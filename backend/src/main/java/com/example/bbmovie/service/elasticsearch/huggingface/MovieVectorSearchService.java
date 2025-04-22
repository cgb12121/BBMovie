package com.example.bbmovie.service.elasticsearch.huggingface;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.bbmovie.model.Movie;
import com.example.bbmovie.model.elasticsearch.MovieVectorDocument;
import com.example.bbmovie.service.embedding.HuggingFaceEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service(value = "vectorElastic")
@RequiredArgsConstructor
public class MovieVectorSearchService {

   private final ElasticsearchClient elasticsearchClient;
   private final HuggingFaceEmbeddingService huggingFaceEmbeddingService;

   @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
   private String indexName;

   public Object test() throws IOException {
       float[] contentVector = huggingFaceEmbeddingService.generateEmbedding("test");
       MovieVectorDocument doc = MovieVectorDocument.builder()
               .id("test")
               .title("test")
               .description("test")
               .contentVector(contentVector)
               .rating(3.0)
               .categories(List.of("Hehe"))
               .posterUrl("none")
               .releaseDate(LocalDateTime.now())
               .build();
       elasticsearchClient.index(i -> i
               .index(indexName)
               .id(doc.getId())
               .document(doc)
       );
       Object result = elasticsearchClient.get(g -> g
               .index(indexName)
               .id("test")
               , Object.class
       );
       log.info("Indexed document: {}", result);
       return result;
   }

    public List<?> searchSimilarMovies(String query, int limit) throws IOException {
        float[] queryVectorArray = huggingFaceEmbeddingService.generateEmbedding(query);
        log.info("queryVectorArray: {}", queryVectorArray);
        List<Float> queryVector = convertToFloatList(queryVectorArray);
        log.info("queryVector: {}", queryVector);

        SearchResponse<?> response = elasticsearchClient.search(s -> s
                        .index(indexName)
                        .knn(knn -> knn
                                .field("contentVector")
                                .queryVector(queryVector)
                                .k(limit)
                                .numCandidates(limit * 2)
                        )
                        .source(src -> src
                                .filter(f -> f
                                        .excludes("contentVector")
                                )
                        ),
                Object.class
        );

        log.info("response: {}", response);
        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    public List<?> getAllMovies() throws IOException {
        SearchResponse<?> response = elasticsearchClient.search(s -> s
                        .index(indexName)
                        .query(q -> q.matchAll(m -> m))
                        .source(src -> src
                                .filter(f -> f
                                        .excludes("contentVector")
                                )
                        )
                        .size(1000),
                Object.class
        );

        log.info("Raw Elasticsearch response: {}", response.toString());
        if (response.hits().total() != null) {
            log.info("Retrieved {} documents from index {}", response.hits().total().value(), indexName);
        }

        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }


    public void indexMovieWithVector(Movie movie) throws IOException {
       String content = movie.getTitle() + " " + movie.getDescription();
       float[] contentVector = huggingFaceEmbeddingService.generateEmbedding(content);

       MovieVectorDocument document = MovieVectorDocument.builder()
               .id(movie.getId().toString())
               .title(movie.getTitle())
               .description(movie.getDescription())
               .contentVector(contentVector)
               .rating(movie.getRating() != null ? movie.getRating() : 0.0f)
               .categories(movie.getCategories().stream().toList())
               .posterUrl(movie.getPosterUrl())
               .releaseDate(movie.getReleaseDate().atStartOfDay())
               .build();

       elasticsearchClient.index(i -> i
               .index(indexName)
               .id(document.getId())
               .document(document)
       );
        log.info("Content vector length: {}", contentVector.length);
   }

    private List<Float> convertToFloatList(float[] floats) {
        List<Float> list = new ArrayList<>(floats.length);
        for (float f : floats) {
            list.add(f);
        }
        return list;
    }

    public void indexMovieDocument(MovieVectorDocument doc) throws IOException {
        String content = doc.getTitle() + " " + doc.getDescription();
        float[] contentVector = huggingFaceEmbeddingService.generateEmbedding(content);

        MovieVectorDocument document = MovieVectorDocument.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .contentVector(contentVector)
                .rating(doc.getRating() != null ? doc.getRating() : 0.0f)
                .categories(doc.getCategories().stream().toList())
                .posterUrl(doc.getPosterUrl())
                .releaseDate(doc.getReleaseDate())
                .build();

        elasticsearchClient.index(i -> i
                .index(indexName)
                .id(document.getId())
                .document(document)
        );
    }


    public void deleteAllDocuments() throws IOException {
        elasticsearchClient.deleteByQuery(d -> d
                .index(indexName)
                .query(q -> q
                        .matchAll(m -> m)
                )
        );
    }
}