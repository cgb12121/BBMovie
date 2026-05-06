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
        String ffmpegPath = properties.getFfmpegPath();
        if (ffmpegPath == null || ffmpegPath.isEmpty()) {
            throw new IllegalArgumentException("ffmpegPath configuration is required, please set app.media-processing.ffmpeg-path");
        }
        return new FFmpeg(ffmpegPath);
    }

    @Bean
    public FFprobe processingFfprobe(MediaProcessingProperties properties) throws IOException {
        String ffprobePath = properties.getFfprobePath();
        if (ffprobePath == null || ffprobePath.isEmpty()) {
            throw new IllegalArgumentException("ffprobePath configuration is required, please set app.media-processing.ffprobe-path");
        }
        return new FFprobe(ffprobePath);
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
