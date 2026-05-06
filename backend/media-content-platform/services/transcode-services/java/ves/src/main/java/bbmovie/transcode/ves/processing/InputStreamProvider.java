package bbmovie.transcode.ves.processing;

import bbmovie.transcode.ves.config.MediaProcessingProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InputStreamProvider {

    private final MinioClient minioClient;
    private final MediaProcessingProperties properties;

    public String presignSourceGetUrl(String bucket, String key) throws Exception {
        int expirySeconds = Math.max(300, properties.getStreamPresignExpirySeconds());
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(key)
                        .expiry(expirySeconds)
                        .build()
        );
    }
}
