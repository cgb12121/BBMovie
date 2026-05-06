package bbmovie.transcode.ves.processing;

import bbmovie.transcode.contracts.dto.EncodeRequest;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.ves.config.MediaProcessingProperties;
import io.temporal.activity.Activity;
import io.temporal.client.ActivityCompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Core VES encode pipeline: presign source -> run FFmpeg HLS encode -> upload artifacts.
 *
 * <p>Includes bounded retries for transient stream/input issues and heartbeats during encode/upload
 * so long-running activities remain healthy in Temporal.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class EncodingProcessingService {

    private final FFmpeg ffmpeg;
    private final MediaProcessingProperties properties;
    private final InputStreamProvider inputStreamProvider;
    private final HlsUploadService hlsUploadService;
    private final EncodingCommandFactory encodingCommandFactory;

    /**
     * Executes rendition encode with retry envelope.
     *
     * @param request encode request with source location, rendition dimensions, and bitrate hints
     * @return successful rung result or failed marker when all attempts are exhausted
     */
    public RungResultDTO encodeResolution(EncodeRequest request) {
        int maxAttempts = Math.max(1, properties.getStreamRetryAttempts());
        long startedAt = System.nanoTime();
        if (log.isDebugEnabled()) {
            log.debug(
                    "[ves] encode start upload={} resolution={} source={}/{} width={} attempts={}",
                    request.uploadId(),
                    request.resolution(),
                    request.sourceBucket(),
                    request.sourceKey(),
                    request.width(),
                    maxAttempts
            );
        }
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Attempt-level isolation keeps temp workspace and presigned URL lifetimes bounded.
                RungResultDTO result = encodeOnceWithPresignedInput(request, attempt, maxAttempts);
                if (log.isDebugEnabled()) {
                    long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
                    log.debug(
                            "[ves] encode done upload={} resolution={} success={} output={} elapsedMs={} attemptsUsed={}",
                            request.uploadId(),
                            request.resolution(),
                            result.success(),
                            result.playlistPath(),
                            elapsedMs,
                            attempt
                    );
                }
                return result;
            } catch (Exception e) {
                boolean lastAttempt = attempt >= maxAttempts;
                log.warn(
                        "stream encode attempt {}/{} failed for upload={} resolution={}: {}",
                        attempt,
                        maxAttempts,
                        request.uploadId(),
                        request.resolution(),
                        e.getMessage()
                );
                if (lastAttempt) {
                    log.error("encodeResolution exhausted retries for upload={} resolution={}", request.uploadId(), request.resolution(), e);
                    return new RungResultDTO(request.resolution(), "", false);
                }
                // Retry only after configurable cool-down to avoid hot-looping on transient failures.
                sleepBeforeRetry();
            }
        }
        return new RungResultDTO(request.resolution(), "", false);
    }

    /** Runs a single encode attempt using a short-lived presigned source URL and temp workspace. */
    private RungResultDTO encodeOnceWithPresignedInput(EncodeRequest request, int attempt, int maxAttempts) throws Exception {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory(
                    Paths.get(properties.getTempDir()),
                    "ves-encode-" + request.uploadId() + "-" + request.resolution() + "-" + attempt + "-"
            );

            String sourceUrl = inputStreamProvider.presignSourceGetUrl(request.sourceBucket(), request.sourceKey());
            Path outDir = workDir.resolve("hls").resolve(request.resolution());
            Files.createDirectories(outDir);
            Path playlist = outDir.resolve("playlist.m3u8");
            Path segmentPattern = outDir.resolve("seg_%03d.ts");
            if (log.isDebugEnabled()) {
                log.debug(
                        "[ves] attempt={}/{} upload={} resolution={} workDir={} outDir={}",
                        attempt,
                        maxAttempts,
                        request.uploadId(),
                        request.resolution(),
                        workDir,
                        outDir
                );
            }
            if (log.isTraceEnabled()) {
                log.trace(
                        "[ves] ffmpeg input url (presigned) upload={} resolution={} url={}",
                        request.uploadId(),
                        request.resolution(),
                        sourceUrl
                );
            }

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            var builder = encodingCommandFactory.buildHlsStreamEncode(request, sourceUrl, playlist, segmentPattern);
            FFmpegJob job = executor.createJob(builder, progress -> {
                try {
                    // Heartbeat by encoded timestamp so Temporal sees steady progress on long encodes.
                    Activity.getExecutionContext().heartbeat(progress.out_time_ns);
                } catch (ActivityCompletionException e) {
                    log.warn("Activity cancelled during heartbeat for progress={}", progress);
                    throw e;
                } catch (Exception e) {
                    log.warn("heartbeat failed for progress={}", progress, e);
                }
            });            
            
            job.run();

            String prefix = properties.getMoviesKeyPrefix() + "/" + request.uploadId() + "/" + request.resolution() + "/";
            hlsUploadService.uploadTree(properties.getHlsBucket(), outDir, prefix);
            log.info(
                    "stream encode succeeded for upload={} resolution={} attempt={}/{}",
                    request.uploadId(),
                    request.resolution(),
                    attempt,
                    maxAttempts
            );
            return new RungResultDTO(request.resolution(), prefix + "playlist.m3u8", true);
        } finally {
            if (workDir != null) {
                try {
                    // Temp directory cleanup is best-effort and should not mask encode outcome.
                    FileSystemUtils.deleteRecursively(workDir);
                } catch (IOException e) {
                    log.error("deleteRecursively failed for workDir={}", workDir, e);
                }
            }
        }
    }

    /** Sleeps between attempts according to configured backoff; preserves interrupt status. */
    private void sleepBeforeRetry() {
        long millis = Math.max(0, properties.getStreamRetryBackoffMillis());
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error("sleepBeforeRetry failed for millis={}", millis, e);
            Thread.currentThread().interrupt();
        }
    }
}
