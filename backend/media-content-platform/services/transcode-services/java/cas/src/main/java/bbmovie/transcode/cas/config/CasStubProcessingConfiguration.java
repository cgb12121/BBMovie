package bbmovie.transcode.cas.config;

import bbmovie.transcode.cas.processing.CasProcessingService;
import bbmovie.transcode.cas.processing.CasStubProcessingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "cas.processing.enabled", havingValue = "false")
public class CasStubProcessingConfiguration {

    @Bean
    public CasProcessingService casProcessingService() {
        return new CasStubProcessingService();
    }
}
