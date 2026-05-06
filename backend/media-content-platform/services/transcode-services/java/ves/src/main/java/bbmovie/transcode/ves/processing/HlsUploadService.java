package bbmovie.transcode.ves.processing;

import bbmovie.transcode.ves.config.MediaProcessingProperties;
import io.temporal.activity.Activity;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
/**
 * Uploads generated HLS playlist/segment tree to object storage.
 *
 * <p>Uses configurable parallelism with virtual-thread executor and activity heartbeats per file
 * upload to keep long uploads observable by Temporal.</p>
 */
public class HlsUploadService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final Tika TIKA = new Tika();

    private static final Map<String, String> HLS_CONTENT_TYPES = Map.of("m3u8", "application/vnd.apple.mpegurl","ts", "video/mp2t");

    private final MinioClient minioClient;
    private final MediaProcessingProperties properties;

    /**
     * Uploads every regular file under {@code root} into {@code bucket/keyPrefix}.
     *
     * @param bucket target object bucket
     * @param root local root directory containing generated HLS artifacts
     * @param keyPrefix destination key prefix (rendition-scoped path)
     */
    public void uploadTree(String bucket, Path root, String keyPrefix) throws Exception {
        String normalizedPrefix = keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
        List<Path> files = listFiles(root);
        if (files.isEmpty()) {
            return;
        }

        int parallelism = Math.max(1, properties.getUploadParallelism());
        try (ExecutorService executor = Executors.newFixedThreadPool(parallelism, Thread.ofVirtual().factory())) {
            List<Future<Void>> futures = new ArrayList<>(files.size());
            for (Path file : files) {
                futures.add(executor.submit(() -> {
                    uploadSingleFile(bucket, root, normalizedPrefix, file);
                    return null;
                }));
            }
            awaitAllUploads(futures);
        }
    }

    /** Lists files deterministically so uploads and logs are stable across runs. */
    private static List<Path> listFiles(Path root) throws Exception {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).sorted().toList();
        }
    }

    /** Uploads one file and emits heartbeat marker with object key. */
    private void uploadSingleFile(String bucket, Path root, String normalizedPrefix, Path file) throws Exception {
        String relative = root.relativize(file).toString().replace('\\', '/');
        String objectKey = normalizedPrefix + relative;
        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .filename(file.toString())
                        .contentType(guessContentType(file))
                        .build()
        );
        try {
            Activity.getExecutionContext().heartbeat("uploaded:" + objectKey);
        } catch (Exception e) {
            log.error("heartbeat failed for objectKey={}", objectKey, e);
        }
    }

    /** Waits for all parallel upload tasks and rethrows original upload exceptions. */
    private static void awaitAllUploads(List<Future<Void>> futures) throws Exception {
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                log.error("awaitAllUploads failed for future={}", future, e);
                Throwable cause = e.getCause();
                if (cause instanceof Exception ex) {
                    throw ex;
                }
                throw new RuntimeException("awaitAllUploads failed for future=" + future, cause);
            }
        }
    }

    /** Infers object content type with Tika + HLS extension fallback table. */
    private static String guessContentType(Path file) {
        String fileName = file.getFileName().toString();
        String detected = TIKA.detect(fileName);
        if (detected != null && !detected.isBlank() && !DEFAULT_CONTENT_TYPE.equalsIgnoreCase(detected)) {
            return detected;
        }

        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return DEFAULT_CONTENT_TYPE;
        }

        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return HLS_CONTENT_TYPES.getOrDefault(extension, DEFAULT_CONTENT_TYPE);
    }
}
