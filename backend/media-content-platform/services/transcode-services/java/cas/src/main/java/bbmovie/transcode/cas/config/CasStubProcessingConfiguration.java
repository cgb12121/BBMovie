package bbmovie.transcode.cas.config;

import bbmovie.transcode.cas.processing.CasProcessingService;
import bbmovie.transcode.cas.processing.CasStubProcessingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "cas.processing.enabled", havingValue = "false")
/** Wiring for stub CAS processing mode used in local/dev flows without external dependencies. */
public class CasStubProcessingConfiguration {

    @Bean
    /** Provides no-op/stub implementation of CAS processing contract. */
    public CasProcessingService casProcessingService() {
        return new CasStubProcessingService();
    }
}
