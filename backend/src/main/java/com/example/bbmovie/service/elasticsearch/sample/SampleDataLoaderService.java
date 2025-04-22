package com.example.bbmovie.service.elasticsearch.sample;

import com.example.bbmovie.model.elasticsearch.MovieVectorDocument;
import com.example.bbmovie.service.elasticsearch.MovieVectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class SampleDataLoaderService {

    private final MovieVectorSearchService movieVectorSearchService;

    public void loadSampleData() {
        try {
            log.info("Starting to load sample vector data for Elasticsearch...");

            List<MovieVectorDocument> documents = SampleData.getSampleVectorDocuments();

            for (MovieVectorDocument doc : documents) {
                try {
                    movieVectorSearchService.indexMovieDocument(doc);
                    log.info("Indexed document: {}", doc.getTitle());
                } catch (Exception e) {
                    log.error("Failed to index movie vector document: {}", doc.getTitle(), e);
                }
            }

            log.info("Sample vector data loaded successfully");
        } catch (Exception e) {
            log.error("Error loading vector sample data", e);
            throw e;
        }
    }

    public void cleanSampleData() throws IOException {
        try {
            log.info("Cleaning up sample vector documents in Elasticsearch...");
            movieVectorSearchService.deleteAllDocuments();
            log.info("Elasticsearch cleanup complete.");
        } catch (Exception e) {
            log.error("Error during Elasticsearch cleanup", e);
            throw e;
        }
    }
}
