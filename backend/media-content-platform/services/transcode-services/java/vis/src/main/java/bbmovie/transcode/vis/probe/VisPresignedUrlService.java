package bbmovie.transcode.vis.probe;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Ported from transcode-worker {@code PresignedUrlService} (GET presign for ffprobe).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisPresignedUrlService {

    private final MinioClient minioClient;

    @Value("${app.minio.presigned-url-expiry-minutes:60}")
    private int defaultExpiryMinutes;

    public String generateGetUrl(String bucket, String key, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}/{}", bucket, key, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    public String generateProbeUrl(String bucket, String key) {
        return generateGetUrl(bucket, key, 5);
    }
}
