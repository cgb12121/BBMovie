package bbmovie.transcode.temporal_orchestrator.config;

import bbmovie.transcode.contracts.activity.MediaActivities;
import bbmovie.transcode.temporal_orchestrator.processing.ProcessingMediaActivities;
import io.minio.MinioClient;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(name = "temporal.activity-implementation", havingValue = "processing")
@EnableConfigurationProperties(MediaProcessingProperties.class)
public class MediaProcessingConfiguration {

    @Bean
    public MinioClient processingMinioClient(
            @Value("${minio.url}") String url,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public FFmpeg processingFfmpeg(MediaProcessingProperties properties) throws IOException {
        return new FFmpeg(properties.getFfmpegPath());
    }

    @Bean
    public FFprobe processingFfprobe(MediaProcessingProperties properties) throws IOException {
        return new FFprobe(properties.getFfprobePath());
    }

    @Bean
    public MediaActivities mediaActivities(
            MinioClient processingMinioClient,
            FFmpeg processingFfmpeg,
            FFprobe processingFfprobe,
            MediaProcessingProperties mediaProcessingProperties) {
        return new ProcessingMediaActivities(
                processingMinioClient,
                processingFfmpeg,
                processingFfprobe,
                mediaProcessingProperties
        );
    }
}
