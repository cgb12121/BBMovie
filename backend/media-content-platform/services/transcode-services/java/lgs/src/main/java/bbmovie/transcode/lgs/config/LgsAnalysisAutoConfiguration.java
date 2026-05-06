package bbmovie.transcode.lgs.config;

import bbmovie.transcode.lgs.analysis.LgsLadderGenerationService;
import bbmovie.transcode.lgs.analysis.LgsResolutionCostCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring auto-configuration wiring LGS analysis services. */
@Configuration
public class LgsAnalysisAutoConfiguration {

    @Bean
    /** Cost table bean used by ladder service for capacity weighting. */
    public LgsResolutionCostCalculator lgsResolutionCostCalculator() {
        return new LgsResolutionCostCalculator();
    }

    @Bean
    /** Main LGS ladder generation service bean. */
    public LgsLadderGenerationService lgsLadderGenerationService(LgsResolutionCostCalculator lgsResolutionCostCalculator) {
        return new LgsLadderGenerationService(lgsResolutionCostCalculator);
    }
}
