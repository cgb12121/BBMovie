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
/** Spring wiring for CAS processing dependencies (MinIO, ffprobe, analyzers, and service facade). */
public class CasProcessingConfiguration {

    @Bean
    /** Dedicated MinIO client bean for CAS IO paths. */
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
    /** ffprobe handle used by CAS source analysis and playlist probing helpers. */
    public FFprobe casFfprobe(CasMediaProcessingProperties properties) throws IOException {
        String ffprobePath = properties.getFfprobePath();
        if (ffprobePath == null || ffprobePath.isEmpty()) {
            throw new IllegalArgumentException("FFprobe path is not set");
        }
        return new FFprobe(ffprobePath);
    }

    @Bean
    /** Resolution cost table used by ladder capacity estimation. */
    public ResolutionCostCalculator casResolutionCostCalculator() {
        return new ResolutionCostCalculator();
    }

    @Bean
    /** CAS ladder service with static presets + hint filtering support. */
    public CasLadderGenerationService casLadderGenerationService(ResolutionCostCalculator casResolutionCostCalculator) {
        return new CasLadderGenerationService(casResolutionCostCalculator);
    }

    @Bean
    /** Legacy analyzer bean kept for fallback compatibility. */
    public ComplexityAnalysisService casComplexityAnalysisService(
            @Value("${cas.complexity.enabled:true}") boolean complexityEnabled) {
        if (complexityEnabled) {
            return new HeuristicComplexityAnalysisService();
        }
        return (uploadId, metadata) -> ComplexityProfile.basic(uploadId);
    }

    @Bean
    /** Primary v2 analyzer used for policy-driven complexity scoring and hints. */
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
    /** CAS processing facade consumed by Temporal activities. */
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
