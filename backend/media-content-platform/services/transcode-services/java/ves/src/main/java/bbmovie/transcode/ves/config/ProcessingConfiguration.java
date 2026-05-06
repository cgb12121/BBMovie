package bbmovie.transcode.ves.config;

import bbmovie.transcode.ves.processing.EncodingProcessingService;
import bbmovie.transcode.ves.processing.EncodingCommandFactory;
import bbmovie.transcode.ves.processing.HlsUploadService;
import bbmovie.transcode.ves.processing.InputStreamProvider;
import io.minio.MinioClient;
import net.bramp.ffmpeg.FFmpeg;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/** Spring wiring for VES encode pipeline dependencies. */
@Configuration
@EnableConfigurationProperties(MediaProcessingProperties.class)
public class ProcessingConfiguration {

    @Bean
    /** Shared MinIO client used for source-url presign and output uploads. */
    public MinioClient minioClient(
            @Value("${minio.url}") String url,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    /** ffmpeg executable handle used by encoding service. */
    public FFmpeg ffmpeg(MediaProcessingProperties properties) throws IOException {
        String ffmpegPath = properties.getFfmpegPath();
        if (ffmpegPath == null || ffmpegPath.isEmpty()) {
            throw new IllegalArgumentException("ffmpegPath configuration is required, please set app.media-processing.ffmpeg-path");
        }
        return new FFmpeg(ffmpegPath);
    }

    @Bean
    /** Source-input helper providing presigned URLs for ffmpeg streaming. */
    public InputStreamProvider inputStreamProvider(
            MinioClient minioClient,
            MediaProcessingProperties mediaProcessingProperties) {
        return new InputStreamProvider(minioClient, mediaProcessingProperties);
    }

    @Bean
    /** HLS artifact upload helper with configurable parallelism. */
    public HlsUploadService hlsUploadService(MinioClient minioClient, MediaProcessingProperties mediaProcessingProperties) {
        return new HlsUploadService(minioClient, mediaProcessingProperties);
    }

    @Bean
    /** FFmpeg command builder encapsulating bitrate strategy handling. */
    public EncodingCommandFactory encodingCommandFactory(MediaProcessingProperties mediaProcessingProperties) {
        return new EncodingCommandFactory(mediaProcessingProperties);
    }

    @Bean
    /** Core encoding service composing presign, command build, encode run, and upload steps. */
    public EncodingProcessingService encodingProcessingService(
            FFmpeg ffmpeg,
            InputStreamProvider inputStreamProvider,
            HlsUploadService hlsUploadService,
            EncodingCommandFactory encodingCommandFactory,
            MediaProcessingProperties mediaProcessingProperties) {
        return new EncodingProcessingService(
                ffmpeg,
                mediaProcessingProperties,
                inputStreamProvider,
                hlsUploadService,
                encodingCommandFactory
        );
    }
}
