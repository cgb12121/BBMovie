package com.example.bbmovie.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.bbmovie.model.Movie;
import com.example.bbmovie.model.elasticsearch.MovieVectorDocument;
import com.example.bbmovie.service.HuggingFaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service(value = "vectorElastic")
@RequiredArgsConstructor
public class MovieVectorSearchService {

   private final ElasticsearchClient elasticsearchClient;
   private final HuggingFaceService huggingFaceService;

   @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
   private String indexName;

    public List<MovieVectorDocument> searchSimilarMovies(String query, int k) throws IOException {
        float[] queryVectorArray = huggingFaceService.generateEmbedding(query);
        log.info("queryVectorArray: {}", queryVectorArray);
        List<Float> queryVector = convertToFloatList(queryVectorArray);
        log.info("queryVector: {}", queryVector);

        SearchResponse<MovieVectorDocument> response = elasticsearchClient.search(s -> s
                        .index(indexName)
                        .knn(knn -> knn
                                .field("content_vector")
                                .queryVector(queryVector)
                                .k(k)
                                .numCandidates(k * 2)
                        ),
                MovieVectorDocument.class
        );

        log.info("response: {}", response);
        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    public List<MovieVectorDocument> getAllMovies() throws IOException {
        SearchResponse<MovieVectorDocument> response = elasticsearchClient.search(s -> s
                        .index(indexName)
                        .query(q -> q.matchAll(m -> m))
                        .size(1000),
                MovieVectorDocument.class
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
       float[] contentVector = huggingFaceService.generateEmbedding(content);

       MovieVectorDocument document = MovieVectorDocument.builder()
               .id(movie.getId().toString())
               .title(movie.getTitle())
               .description(movie.getDescription())
               .contentVector(contentVector)
               .rating(movie.getRating() != null ? movie.getRating() : 0.0f)
               .categories(movie.getCategories().stream().toList())
               .posterUrl(movie.getPosterUrl())
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

}