package bbmovie.transcode.ves.processing;

import bbmovie.transcode.ves.config.MediaProcessingProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;

/**
 * Source input helper for VES encode pipeline.
 *
 * <p>Currently exposes presigned GET URL generation so ffmpeg can stream source objects directly
 * without pre-downloading to local disk.</p>
 */
@RequiredArgsConstructor
public class InputStreamProvider {

    private final MinioClient minioClient;
    private final MediaProcessingProperties properties;

    /**
     * Generates bounded-lifetime presigned URL for source object streaming.
     *
     * @param bucket source bucket
     * @param key source object key
     * @return presigned GET URL suitable for ffmpeg input
     */
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
