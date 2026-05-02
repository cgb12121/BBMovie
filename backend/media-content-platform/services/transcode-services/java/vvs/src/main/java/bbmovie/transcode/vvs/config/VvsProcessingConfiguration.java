package bbmovie.transcode.vvs.config;

import bbmovie.transcode.vvs.processing.VvsQualityProcessingService;
import io.minio.MinioClient;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(VvsMediaProcessingProperties.class)
public class VvsProcessingConfiguration {

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

    @Bean
    public FFprobe vvsFfprobe(VvsMediaProcessingProperties properties) throws IOException {
        return new FFprobe(properties.getFfprobePath());
    }

    @Bean
    public VvsQualityProcessingService vvsQualityProcessingService(
            MinioClient vvsMinioClient,
            FFprobe vvsFfprobe,
            VvsMediaProcessingProperties vvsMediaProcessingProperties) {
        return new VvsQualityProcessingService(vvsMinioClient, vvsFfprobe, vvsMediaProcessingProperties);
    }
}
