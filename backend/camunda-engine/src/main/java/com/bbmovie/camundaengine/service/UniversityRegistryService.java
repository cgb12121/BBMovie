package com.bbmovie.camundaengine.service;

import com.bbmovie.camundaengine.dto.UniversityObject;
import com.bbmovie.camundaengine.entity.University;
import com.bbmovie.camundaengine.repository.UniversityRepository;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Log4j2
@Service
public class UniversityRegistryService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final UniversityRepository universityRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public UniversityRegistryService(UniversityRepository universityRepository, EntityManager entityManager) {
        this.universityRepository = universityRepository;
        this.entityManager = entityManager;
        this.objectMapper = new ObjectMapper();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        long startTime = System.nanoTime();
        long count = universityRepository.count();
        log.info("Found {} entries in university registry at the database", count);
        if (count > 0) {
            log.info("University registry already contains {} entries, skipping initialization", count);
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource("data/uni.json");
            if (!resource.exists()) {
                log.warn("University registry data file (uni.json) not found in classpath:data/");
                return;
            }
            JsonFactory jsonFactory = new JsonFactory();
            int savedCount = 0;
            int batchSize = 50;
            List<University> batch = new ArrayList<>(batchSize);

            try (JsonParser parser = jsonFactory.createParser(resource.getInputStream())) {
                if (parser.nextToken() == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        UniversityObject uniObject = objectMapper.readValue(parser, UniversityObject.class);
                        University university = convert(uniObject);
                        batch.add(university);

                        if (batch.size() >= batchSize) {
                            universityRepository.saveAllAndFlush(batch);
                            entityManager.clear();
                            savedCount += batch.size();
                            batch.clear();
                        }
                    }
                    if (!batch.isEmpty()) {
                        universityRepository.saveAllAndFlush(batch);
                        entityManager.clear();
                        savedCount += batch.size();
                    }
                }
            }

            log.info("Initialized university registry with {} entries", savedCount);
        } catch (Exception e) {
            log.error("Failed to load university registry from JSON", e);
        } finally {
            long endTime = System.nanoTime();
            log.info("Initialization took {} ms", (endTime - startTime) / 1000000);
        }
    }

    public Optional<University> findByDomain(String query) {
        if (query.contains("@"))
            query = extractDomain(query);
        return universityRepository.findByDomainsContainingIgnoreCase(query);
    }

    public Optional<University> bestMatchByName(String text) {
        return universityRepository.findByNameContainingIgnoreCase(text);
    }

    private String extractDomain(String emailOrText) {
        int atIndex = emailOrText.lastIndexOf('@');
        if (atIndex >= 0 && atIndex < emailOrText.length() - 1) {
            return emailOrText.substring(atIndex + 1).trim().toLowerCase();
        }
        return null;
    }

    private University convert(UniversityObject uniObject) {
        return University.builder()
                .name(uniObject.getName())
                .domains(uniObject.getDomains() != null ? String.join(",", uniObject.getDomains()) : null)
                .webPages(uniObject.getWeb_pages() != null ? String.join(",", uniObject.getWeb_pages()) : null)
                .country(uniObject.getCountry())
                .alphaTwoCode(uniObject.getAlpha_two_code())
                .stateProvince(uniObject.getState_province())
                .build();
    }
}
