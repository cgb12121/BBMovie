package bbmovie.transcode.lgs.config;

import bbmovie.transcode.lgs.analysis.LgsLadderGenerationService;
import bbmovie.transcode.lgs.analysis.LgsResolutionCostCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LgsAnalysisAutoConfiguration {

    @Bean
    public LgsResolutionCostCalculator lgsResolutionCostCalculator() {
        return new LgsResolutionCostCalculator();
    }

    @Bean
    public LgsLadderGenerationService lgsLadderGenerationService(LgsResolutionCostCalculator lgsResolutionCostCalculator) {
        return new LgsLadderGenerationService(lgsResolutionCostCalculator);
    }
}
