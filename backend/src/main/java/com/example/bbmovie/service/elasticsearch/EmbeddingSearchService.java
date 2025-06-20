package com.example.bbmovie.service.elasticsearch;

import ai.djl.translate.TranslateException;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.ScriptLanguage;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.example.bbmovie.entity.Movie;
import com.example.bbmovie.entity.elasticsearch.MovieVectorDocument;
import com.example.bbmovie.service.embedding.LocalEmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Service(value = "vectorElastic")
public class EmbeddingSearchService {

   private final ElasticsearchClient elasticsearchClient;
   private final LocalEmbeddingService localEmbeddingService;

   @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
   private String indexName;

   @Autowired
    public EmbeddingSearchService(
            ElasticsearchClient elasticsearchClient,
            LocalEmbeddingService localEmbeddingService
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.localEmbeddingService = localEmbeddingService;
    }

    public List<?> searchSimilarMovies(String query, int limit) throws IOException, TranslateException {
        float[] queryVectorArray = localEmbeddingService.generateEmbedding(query);
        log.info("queryVectorArray: {}", queryVectorArray);
        List<Float> queryVector = convertToFloatList(queryVectorArray);
        log.info("queryVector: {}", queryVector);

        SearchResponse<?> response = elasticsearchClient.search(searchRequest -> searchRequest
                        .index(indexName)
                        .knn(knn -> knn
                                .field(QueryEmbeddingField.EMBEDDING_FIELD)
                                .queryVector(queryVector)
                                .k(limit)
                                .numCandidates(limit * 2)
                        )
                        .source(src -> src
                                .filter(f -> f
                                        .excludes(QueryEmbeddingField.EMBEDDING_FIELD)
                                )
                        ),
                Object.class
        );

        //This will try to make the object JSON so we can try to make java object to replace ?
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String jsonString = objectMapper.writeValueAsString(response);

        log.info("response: {}", jsonString);
        return response.hits().hits().stream()
                .map(Hit::source)
                .toList();
    }

    public List<?> getAllMovies() throws IOException {
        SearchResponse<?> response = elasticsearchClient.search(searchRequest -> searchRequest
                        .index(indexName)
                        .query(q -> q.matchAll(m -> m))
                        .source(src -> src
                                .filter(f -> f
                                        .excludes(QueryEmbeddingField.EMBEDDING_FIELD)
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
                .toList();
    }

    public void indexMovieWithVector(Movie movie) throws IOException, TranslateException {
       String content = movie.getTitle() + " " + movie.getDescription();
       float[] contentVector = localEmbeddingService.generateEmbedding(content);

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

       elasticsearchClient.index(indexRequest -> indexRequest
               .index(indexName)
               .id(document.getId())
               .document(document)
       );
        log.info("Content vector length: {}", contentVector.length);
   }

    public boolean deleteMovieIfExists(String movieId) throws IOException {
        var response = elasticsearchClient.exists(e -> e
                .index(indexName)
                .id(movieId)
        );
        if (response.value()) {
            elasticsearchClient.delete(d -> d.index(indexName).id(movieId));
            log.info("Deleted document with ID {}", movieId);
            return true;
        } else {
            log.warn("Document with ID {} does not exist", movieId);
            return false;
        }
    }

    public void updateRatingWithScript(String movieId, double newRating) throws IOException {
        Script script = new Script.Builder()
                .lang(ScriptLanguage.Painless.jsonValue())
                .source("ctx._source.rating = params.rating")
                .params(Map.of("rating", JsonData.of(newRating)))
                .build();

        UpdateResponse<Object> response = elasticsearchClient.update(u -> u
                        .index(indexName)
                        .id(movieId)
                        .script(script),
                Object.class
        );

        log.info("Updated rating for movie {} using script. Result: {}", movieId, response.result());
    }

    public void updateMoviePartial(String movieId, Map<String, Object> fieldsToUpdate) throws IOException {
        elasticsearchClient.update(updateRequest -> updateRequest
                        .index(indexName)
                        .id(movieId)
                        .doc(fieldsToUpdate),
                Object.class
        );

        log.info("Updated document partially {} with fields {}", movieId, fieldsToUpdate);
    }

    public void updateMovieEntirely(String movieId, Map<String, Object> fieldsToUpdate) throws IOException {
        elasticsearchClient.update(updateRequest -> updateRequest
                        .index(indexName)
                        .id(movieId)
                        .doc(fieldsToUpdate),
                Object.class
        );

        log.info("Updated document entirely {} with fields {}", movieId, fieldsToUpdate);
    }


    public void deleteMovieById(String movieId) throws IOException {
        elasticsearchClient.delete(deleteRequest -> deleteRequest
                .index(indexName)
                .id(movieId)
        );

        log.info("Deleted document with ID {} from index {}", movieId, indexName);
    }

    public void deleteAllDocuments() throws IOException {
        elasticsearchClient.deleteByQuery(d -> d
                .index(indexName)
                .query(q -> q
                        .matchAll(m -> m)
                )
        );
    }

    private List<Float> convertToFloatList(float[] floats) {
        List<Float> list = new ArrayList<>(floats.length);
        for (float f : floats) {
            list.add(f);
        }
        return list;
    }
}