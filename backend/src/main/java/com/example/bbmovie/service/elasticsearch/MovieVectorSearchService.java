package com.example.bbmovie.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.example.bbmovie.model.Movie;
import com.example.bbmovie.model.elasticsearch.MovieVectorDocument;
import com.example.bbmovie.service.impl.HuggingFaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service(value = "vectorElastic")
@RequiredArgsConstructor
public class MovieVectorSearchService {

   private final ElasticsearchClient elasticsearchClient;
   private final HuggingFaceService huggingFaceService;

//   @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
   private final String indexName = "movies";

   public List<MovieVectorDocument> searchSimilarMovies(String query, int k) throws IOException {
       float[] queryVector = huggingFaceService.generateEmbedding(query);

       SearchResponse<MovieVectorDocument> response = elasticsearchClient.search(s -> s
                       .index(indexName)
                       .query(q -> q
                               .scriptScore(ss -> ss
                                       .query(innerQ -> innerQ.matchAll(m -> m))
                                       .script(script -> script
                                               .inline(inline -> inline
                                                       .source("cosineSimilarity(params.queryVector, 'contentVector') + 1.0")
                                                       .lang("painless")
                                                       .params(Map.of("queryVector", JsonData.of(convertToDoubleList(queryVector))))
                                               )
                                       )
                               )
                       )
                       .size(k),
               MovieVectorDocument.class
       );

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
               .rating(movie.getRating())
               .categories(movie.getCategories().stream().toList())
               .posterUrl(movie.getPosterUrl())
               .build();

       elasticsearchClient.index(i -> i
               .index(indexName)
               .id(document.getId())
               .document(document)
       );
   }

   private List<Double> convertToDoubleList(float[] floats) {
       List<Double> list = new ArrayList<>(floats.length);
       for (float f : floats) {
           list.add((double) f);
       }
       return list;
   }
}