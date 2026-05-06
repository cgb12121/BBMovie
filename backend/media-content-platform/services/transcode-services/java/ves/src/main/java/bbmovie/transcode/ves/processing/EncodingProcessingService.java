package bbmovie.transcode.ves.processing;

import bbmovie.transcode.contracts.dto.EncodeRequest;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.ves.config.MediaProcessingProperties;
import io.temporal.activity.Activity;
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

@Slf4j
@RequiredArgsConstructor
public class EncodingProcessingService {

    private final FFmpeg ffmpeg;
    private final MediaProcessingProperties properties;
    private final InputStreamProvider inputStreamProvider;
    private final HlsUploadService hlsUploadService;
    private final EncodingCommandFactory encodingCommandFactory;

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
                sleepBeforeRetry();
            }
        }
        return new RungResultDTO(request.resolution(), "", false);
    }

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
                    Activity.getExecutionContext().heartbeat(progress.out_time_ns);
                } catch (Exception e) {
                    log.error("heartbeat failed for progress={}", progress, e);
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
                    FileSystemUtils.deleteRecursively(workDir);
                } catch (IOException e) {
                    log.error("deleteRecursively failed for workDir={}", workDir, e);
                }
            }
        }
    }

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
