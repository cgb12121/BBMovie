package bbmovie.transcode.cas.config;

import bbmovie.transcode.cas.analysis.CasLadderGenerationService;
import bbmovie.transcode.cas.analysis.ComplexityAnalysisService;
import bbmovie.transcode.cas.analysis.ComplexityAnalysisV2Service;
import bbmovie.transcode.cas.analysis.ComplexityProfile;
import bbmovie.transcode.cas.analysis.DecisionHintsPolicyEngine;
import bbmovie.transcode.cas.analysis.HeuristicComplexityAnalysisService;
import bbmovie.transcode.cas.analysis.ResolutionCostCalculator;
import bbmovie.transcode.cas.analysis.VectorComplexityAnalysisService;
import bbmovie.transcode.cas.processing.CasMinioProbeManifestService;
import bbmovie.transcode.cas.processing.CasProfileCompatibilityAdapter;
import bbmovie.transcode.cas.processing.CasProcessingService;
import io.minio.MinioClient;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(name = "cas.processing.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CasMediaProcessingProperties.class)
public class CasProcessingConfiguration {

    @Bean
    public MinioClient casMinioClient(
            @Value("${minio.url}") String url,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public FFprobe casFfprobe(CasMediaProcessingProperties properties) throws IOException {
        return new FFprobe(properties.getFfprobePath());
    }

    @Bean
    public ResolutionCostCalculator casResolutionCostCalculator() {
        return new ResolutionCostCalculator();
    }

    @Bean
    public CasLadderGenerationService casLadderGenerationService(ResolutionCostCalculator casResolutionCostCalculator) {
        return new CasLadderGenerationService(casResolutionCostCalculator);
    }

    @Bean
    public ComplexityAnalysisService casComplexityAnalysisService(
            @Value("${cas.complexity.enabled:true}") boolean complexityEnabled) {
        if (complexityEnabled) {
            return new HeuristicComplexityAnalysisService();
        }
        return (uploadId, metadata) -> ComplexityProfile.basic(uploadId);
    }

    @Bean
    public ComplexityAnalysisV2Service casComplexityAnalysisV2Service(
            DecisionHintsPolicyEngine decisionHintsPolicyEngine,
            CasMediaProcessingProperties casMediaProcessingProperties) {
        return new VectorComplexityAnalysisService(
                decisionHintsPolicyEngine,
                casMediaProcessingProperties.getProfileV2AnalysisVersion(),
                casMediaProcessingProperties.getProfileV2PolicyVersion()
        );
    }

    @Bean
    public CasProcessingService casProcessingService(
            MinioClient casMinioClient,
            FFprobe casFfprobe,
            CasMediaProcessingProperties casMediaProcessingProperties,
            ComplexityAnalysisService casComplexityAnalysisService,
            ComplexityAnalysisV2Service casComplexityAnalysisV2Service,
            CasLadderGenerationService casLadderGenerationService,
            CasProfileCompatibilityAdapter casProfileCompatibilityAdapter) {
        return new CasMinioProbeManifestService(
                casMinioClient,
                casFfprobe,
                casMediaProcessingProperties,
                casComplexityAnalysisService,
                casComplexityAnalysisV2Service,
                casLadderGenerationService,
                casProfileCompatibilityAdapter
        );
    }
}
