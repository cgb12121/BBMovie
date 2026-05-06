package bbmovie.transcode.vis.probe;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Ported from transcode-worker {@code MinioDownloadService#downloadPartial}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisMinioPartialDownloadService {

    private final MinioClient minioClient;

    public byte[] downloadPartial(String bucket, String key, long maxBytes) {
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .offset(0L)
                        .length(maxBytes)
                        .build())) {
            return response.readAllBytes();
        } catch (Exception e) {
            log.error("Failed partial download {}/{}", bucket, key, e);
            throw new RuntimeException("Failed to download partial content", e);
        }
    }
}
