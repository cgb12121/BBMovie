package bbmovie.transcode.vis.config;

import bbmovie.transcode.vis.probe.VisLadderGenerationService;
import bbmovie.transcode.vis.probe.VisResolutionCostCalculator;
import io.minio.MinioClient;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/** Spring wiring for VIS probing dependencies and ladder helpers. */
@Configuration
@EnableConfigurationProperties(VisMediaProcessingProperties.class)
public class VisProcessingConfiguration {

    /** MinIO client used by VIS probe strategies. */
    @Bean
    public MinioClient visMinioClient(
            @Value("${minio.url}") String url,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    /** ffprobe executable handle used by VIS metadata service. */
    @Bean
    public FFprobe visFfprobe(VisMediaProcessingProperties properties) throws IOException {
        String ffprobePath = properties.getFfprobePath();
        if (ffprobePath == null || ffprobePath.isEmpty()) {
            throw new IllegalArgumentException("FFprobe path is not set");
        }
        return new FFprobe(ffprobePath);
    }

    /** Resolution cost table used by VIS ladder planning. */
    @Bean
    public VisResolutionCostCalculator visResolutionCostCalculator() {
        return new VisResolutionCostCalculator();
    }

    /** VIS ladder generation service bean for probe outcome planning. */
    @Bean
    public VisLadderGenerationService visLadderGenerationService(VisResolutionCostCalculator visResolutionCostCalculator) {
        return new VisLadderGenerationService(visResolutionCostCalculator);
    }
}
