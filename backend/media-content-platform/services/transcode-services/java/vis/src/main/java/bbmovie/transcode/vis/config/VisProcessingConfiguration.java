package bbmovie.transcode.vis.config;

import io.minio.MinioClient;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(VisMediaProcessingProperties.class)
public class VisProcessingConfiguration {

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

    @Bean
    public FFprobe visFfprobe(VisMediaProcessingProperties properties) throws IOException {
        return new FFprobe(properties.getFfprobePath());
    }
}
