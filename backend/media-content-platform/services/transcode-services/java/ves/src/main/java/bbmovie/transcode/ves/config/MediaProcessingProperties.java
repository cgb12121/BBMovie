package bbmovie.transcode.ves.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.media-processing")
public class MediaProcessingProperties {

    private String hlsBucket = "bbmovie-hls";
    private String moviesKeyPrefix = "movies";
    private String ffmpegPath = "ffmpeg";
    /**
     * FFmpeg thread count per encode task.
     * Keep this smaller than total CPU cores to allow multiple encodes in parallel.
     */
    private int ffmpegThreads = 2;
    /**
     * Parallel uploads for HLS segments/playlist to object storage.
     */
    private int uploadParallelism = 8;
    /**
     * Presigned URL validity for stream input from object storage.
     */
    private int streamPresignExpirySeconds = 21600;
    /**
     * Retry attempts inside one activity when stream input fails transiently.
     */
    private int streamRetryAttempts = 3;
    /**
     * Backoff between in-node stream retry attempts.
     */
    private int streamRetryBackoffMillis = 3000;
    private String tempDir = System.getProperty("java.io.tmpdir");
}
