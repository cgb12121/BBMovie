package com.example.bbmoviesearch.service.elasticsearch;

import com.example.bbmoviesearch.dto.PageResponse;
import com.example.bbmoviesearch.dto.SearchCriteria;
import com.example.bbmoviesearch.entity.MovieDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Lazy
@Service
@Log4j2
@ConditionalOnMissingBean(SpringESSearchService.class)
public class SpringESSearchService implements SearchService {

    private final VectorStore vectorStore;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public SpringESSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public Flux<MovieDocument> searchSimilar(String query, int topK) {
        return Mono.<List<MovieDocument>>fromCallable(() -> {
            List<Document> result = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .build()
            );
            if (result == null) {
                return List.of();
            }
            return result.stream()
                    .map(doc -> convertMetadata(doc.getMetadata(), MovieDocument.class))
                    .toList();
        }).flatMapMany(Flux::fromIterable);
    }

    @SuppressWarnings("all")
    private <T> T convertMetadata(Map<String, Object> meta, Class<T> clazz) {
        return mapper.convertValue(meta, clazz);
    }

    @Override
    public <T> Mono<PageResponse<T>> getAllMovies(int page, int size, int age, String region, Class<T> clazz) {
        return null;
    }

    @Override
    public <T> Mono<PageResponse<T>> searchSimilar(SearchCriteria criteria, Class<T> clazz) {
        return null;
    }
}
