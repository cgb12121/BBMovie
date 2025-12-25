package com.bbmovie.transcodeworker.service.processing;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for selecting the appropriate MediaProcessor based on UploadPurpose.
 * Uses Spring's dependency injection to collect all MediaProcessor implementations.
 */
@Slf4j
@Component
public class MediaProcessorFactory {

    private final List<MediaProcessor> processors;

    /**
     * Constructor injection collects all MediaProcessor beans.
     */
    public MediaProcessorFactory(List<MediaProcessor> processors) {
        this.processors = processors;
        log.info("Registered {} media processors: {}",
                processors.size(),
                processors.stream().map(p -> p.getClass().getSimpleName()).toList());
    }

    /**
     * Gets the appropriate processor for the given purpose.
     *
     * @param purpose Upload purpose to find processor for
     * @return MediaProcessor that handles this purpose
     * @throws IllegalArgumentException if no processor found
     */
    public MediaProcessor getProcessor(UploadPurpose purpose) {
        return processors.stream()
                .filter(p -> p.supports(purpose))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No processor found for purpose: " + purpose));
    }

    /**
     * Checks if a processor exists for the given purpose.
     */
    public boolean hasProcessor(UploadPurpose purpose) {
        return processors.stream().anyMatch(p -> p.supports(purpose));
    }

    /**
     * Gets all registered processors.
     */
    public List<MediaProcessor> getAllProcessors() {
        return List.copyOf(processors);
    }
}

