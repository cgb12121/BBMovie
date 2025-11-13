package com.bbmovie.search.repository.esclient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.bbmovie.search.entity.MovieDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Repository
public class MovieMetadataRepositoryImpl implements MovieMetadataRepository {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    @Autowired
    public MovieMetadataRepositoryImpl(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public Mono<MovieDocument> findById(String id) {
        return Mono.fromCallable(() -> {
            try {
                return elasticsearchClient.get(g -> g.index(indexName).id(id), MovieDocument.class);
            } catch (ElasticsearchException e) {
                if (e.status() == 404) {
                    return null; // Return null to represent not found, which will be mapped to Mono.empty()
                }
                throw e; // Re-throw other Elasticsearch exceptions
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(response -> {
            if ((response != null && response.found() && response.source() != null)) {
                return Mono.just(response.source());
            }

            return Mono.empty();
        });
    }

    @Override
    public Mono<MovieDocument> save(MovieDocument document) {
        return Mono.fromCallable(() -> {
            elasticsearchClient.index(i -> i
                    .index(indexName)
                    .id(document.getId())
                    .document(document)
            );
            return document; // Return the original document on success
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
