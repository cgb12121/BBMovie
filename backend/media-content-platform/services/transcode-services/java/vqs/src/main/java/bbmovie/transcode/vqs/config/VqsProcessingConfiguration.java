package bbmovie.transcode.vqs.config;

import bbmovie.transcode.vqs.processing.VqsQualityProcessingService;
import io.minio.MinioClient;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/** Spring wiring for VQS processing dependencies (MinIO + ffprobe + quality service). */
@Configuration
@EnableConfigurationProperties(VqsMediaProcessingProperties.class)
public class VqsProcessingConfiguration {

    /** MinIO client used by VQS to read rendition playlists from object storage. */
    @Bean
    public MinioClient vqsMinioClient(
            @Value("${minio.url}") String url,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    /** ffprobe executable handle for rendition validation/scoring checks. */
    @Bean
    public FFprobe vqsFfprobe(VqsMediaProcessingProperties properties) throws IOException {
        String ffprobePath = properties.getFfprobePath();
        if (ffprobePath == null || ffprobePath.isEmpty()) {
            throw new IllegalArgumentException("FFprobe path is not set");
        }
        return new FFprobe(ffprobePath);
    }

    /** Main VQS quality-processing service bean. */
    @Bean
    public VqsQualityProcessingService vqsQualityProcessingService(
            MinioClient vqsMinioClient,
            FFprobe vqsFfprobe,
            VqsMediaProcessingProperties vqsMediaProcessingProperties) {
        return new VqsQualityProcessingService(vqsMinioClient, vqsFfprobe, vqsMediaProcessingProperties);
    }
}
