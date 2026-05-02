package bbmovie.transcode.vqs.config;

import bbmovie.transcode.vqs.processing.VqsQualityProcessingService;
import io.minio.MinioClient;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(VqsMediaProcessingProperties.class)
public class VqsProcessingConfiguration {

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

    @Bean
    public FFprobe vqsFfprobe(VqsMediaProcessingProperties properties) throws IOException {
        return new FFprobe(properties.getFfprobePath());
    }

    @Bean
    public VqsQualityProcessingService vqsQualityProcessingService(
            MinioClient vqsMinioClient,
            FFprobe vqsFfprobe,
            VqsMediaProcessingProperties vqsMediaProcessingProperties) {
        return new VqsQualityProcessingService(vqsMinioClient, vqsFfprobe, vqsMediaProcessingProperties);
    }
}
