package bbmovie.transcode.vvs.config;

import bbmovie.transcode.vvs.processing.VvsQualityProcessingService;
import io.minio.MinioClient;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/** Spring wiring for VVS processing dependencies (MinIO + ffprobe + quality service). */
@Configuration
@EnableConfigurationProperties(VvsMediaProcessingProperties.class)
public class VvsProcessingConfiguration {

    /** MinIO client used by VVS to read rendition playlists from object storage. */
    @Bean
    public MinioClient vvsMinioClient(
            @Value("${minio.url}") String url,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    /** ffprobe executable handle for rendition validation. */
    @Bean
    public FFprobe vvsFfprobe(VvsMediaProcessingProperties properties) throws IOException {
        String ffprobePath = properties.getFfprobePath();
        if (ffprobePath == null || ffprobePath.isEmpty()) {
            throw new IllegalArgumentException("FFprobe path is not set");
        }
        return new FFprobe(ffprobePath);
    }

    /** Main VVS quality-processing service bean. */
    @Bean
    public VvsQualityProcessingService vvsQualityProcessingService(
            MinioClient vvsMinioClient,
            FFprobe vvsFfprobe,
            VvsMediaProcessingProperties vvsMediaProcessingProperties) {
        return new VvsQualityProcessingService(vvsMinioClient, vvsFfprobe, vvsMediaProcessingProperties);
    }
}
