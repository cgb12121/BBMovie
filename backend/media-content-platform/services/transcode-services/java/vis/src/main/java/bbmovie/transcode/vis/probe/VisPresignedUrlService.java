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
 * Helper for generating short-lived presigned GET URLs for VIS probe flows.
 *
 * <p>Ported from transcode-worker {@code PresignedUrlService}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisPresignedUrlService {

    private final MinioClient minioClient;

    @Value("${app.minio.presigned-url-expiry-minutes:60}")
    private int defaultExpiryMinutes;

    /** Creates GET presigned URL for explicit expiry duration. */
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

    /** Creates short-lived URL tuned for probe operations. */
    public String generateProbeUrl(String bucket, String key) {
        return generateGetUrl(bucket, key, 5);
    }
}
