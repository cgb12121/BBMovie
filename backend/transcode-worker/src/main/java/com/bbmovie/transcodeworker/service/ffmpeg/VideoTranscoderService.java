package com.bbmovie.transcodeworker.service.ffmpeg;

import com.bbmovie.transcodeworker.exception.FileUploadException;
import com.bbmovie.transcodeworker.service.ffmpeg.option.CodecOptions;
import com.bbmovie.transcodeworker.service.ffmpeg.option.PresetOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bbmovie.transcodeworker.util.Converter.hexStringToByteArray;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTranscoderService {

    @Value("${app.transcode.key-server-url}")
    private String keyServerUrl;

    @Value("${app.transcode.key-rotation-interval:10}")
    private int keyRotationInterval; // number of segments before rotating the key

    private final FFmpeg ffmpeg;
    private final MetadataService metadataService;
    private final EncryptionService encryptionService;

    public record VideoResolution(int width, int height, String filename) {
        public String name() {
            return filename; // e.g., "720p", "1080p"
        }
    }

    public record ResolutionDefinition(int minWidth, int targetWidth, int targetHeight, String suffix) { }

    public static final List<ResolutionDefinition> PREDEFINED_RESOLUTIONS = List.of(
            new ResolutionDefinition(1920, 1920, 1080, "1080p"),
            new ResolutionDefinition(1280, 1280, 720, "720p"),
            new ResolutionDefinition(854, 854, 480, "480p"),
            new ResolutionDefinition(640, 640, 360, "360p"),
            new ResolutionDefinition(426, 426, 240, "240p"),
            new ResolutionDefinition(256, 256, 144, "144p")
    );

    public List<VideoResolution> determineTargetResolutions(FFmpegVideoMetadata meta) {
        List<VideoResolution> targets = new ArrayList<>();
        for (ResolutionDefinition def : PREDEFINED_RESOLUTIONS) {
            if (meta.width() >= def.minWidth()) {
                targets.add(new VideoResolution(def.targetWidth(), def.targetHeight(), def.suffix()));
            }
        }
        if (targets.isEmpty()) {
            targets.add(new VideoResolution(meta.width(), meta.height(), "original"));
        }
        return targets;
    }

    public void transcode(Path input, List<VideoResolution> videoResolutions, String outputDir, String uploadId) {
        FFmpegVideoMetadata meta = metadataService.getMetadata(input);

        FFmpegExecutor executor;
        try {
            executor = new FFmpegExecutor(ffmpeg);
        } catch (IOException e) {
            log.error("Failed to initialize FFmpegExecutor: {}", e.getMessage(), e);
            throw new FileUploadException("Unable to process file.");
        }

        Path outputDirPath = Paths.get(outputDir);

        // Generate Initial Encryption Key & IV (Hex) for the video
        String masterKey = encryptionService.generateRandomHex(16);
        String masterIV = encryptionService.generateRandomHex(16);

        log.info("Generated master encryption - Key: {}, IV: {}",
                masterKey.substring(0, 8) + "...",
                masterIV.substring(0, 8) + "...");

        for (VideoResolution res : videoResolutions) {
            try {
                executeHlsTranscodeJob(executor, input, res, outputDirPath, meta, uploadId, masterKey, masterIV);
            } catch (Exception e) {
                log.error("Failed to transcode file {}: {}", input, e.getMessage(), e);
                throw new RuntimeException("Transcoding failed for resolution " + res.filename(), e);
            }
        }

        createMasterPlaylist(videoResolutions, outputDirPath);
    }

    private void executeHlsTranscodeJob(
            FFmpegExecutor executor, Path input, VideoResolution res, Path outputDir,
            FFmpegVideoMetadata meta, String uploadId, String masterKey, String masterIV) throws IOException {
        // Create folder for resolution: /tmp/uuid/720p/
        Path resolutionDir = outputDir.resolve(res.name());
        try {
            Files.createDirectories(resolutionDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory " + resolutionDir, e);
        }

        // Playlist file: /tmp/uuid/720p/playlist.m3u8
        Path playlistPath = resolutionDir.resolve("playlist.m3u8");

        // Segment pattern: /tmp/uuid/720p/seg_%03d.ts
        String segmentFilename = resolutionDir.resolve("seg_%03d.ts").toString();

        // Calculate estimated number of segments
        int estimatedSegments = (int) Math.ceil(meta.duration() / 10.0); // each segment is 10 seconds
        log.debug("[{}] Estimated segments: {}", res.name(), estimatedSegments);

        // 1. Create actual key files for each segment or every N segment (key rotation)
        List<KeyInfo> keyInfos = generateKeyFiles(resolutionDir, masterKey, masterIV, estimatedSegments);
        log.info("[{}] Generated {} key files", res.name(), keyInfos.size());

        // 2. Create a key info file for FFmpeg
        Path keyInfoPath = resolutionDir.resolve("keyinfo.txt");
        createKeyInfoFile(keyInfoPath, keyInfos, uploadId, res.name());

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(input.toString())
                .overrideOutputFiles(true)
                .addOutput(playlistPath.toString())
                .setVideoCodec(CodecOptions.libx264)
                .setPreset(PresetOptions.veryfast)
                .setAudioCodec("aac")
                .setAudioBitRate(128000)
                .setVideoFilter("scale=" + res.width() + ":-2")
                .setFormat("hls")
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", segmentFilename)
                .addExtraArgs("-hls_playlist_type", "vod")

                // Encryption with a key info file
                .addExtraArgs("-hls_key_info_file", keyInfoPath.toString())
                // Enable periodic key rotation (every N segments)
                .addExtraArgs("-hls_flags", "periodic_rekey")
                // Rekey every X segments (default 10)
                // THÍS ARG IS NOT SUPPORTED IN THIS VERSION
//                .addExtraArgs("-hls_periodic_rekey_interval", String.valueOf(keyRotationInterval))
                .done();

        FFmpegJob job = executor.createJob(builder, progress -> {
            if (meta.duration() > 0) {
                double duration_ns = meta.duration() * 1000000000.0;
                double percentage = progress.out_time_ns / duration_ns;
                log.debug("[{}] Transcoding: {}%", res.name(), String.format("%.2f", percentage * 100));
            }
        });
        job.run();

        // 3. Delete a temporary key info file (no need to upload)
        try {
            Files.deleteIfExists(keyInfoPath);
        } catch (IOException e) {
            log.warn("Failed to delete key info file: {}", keyInfoPath, e);
        }

        // 4. Log generated files
        logGeneratedFiles(resolutionDir);

        // 5. Update playlist URLs
        updatePlaylistUrls(playlistPath, uploadId, res.name(), keyInfos);
    }

    /**
     * Create actual key files and return the list of key info
     */
    private List<KeyInfo> generateKeyFiles(Path resolutionDir, String masterKey, String masterIV, int estimatedSegments) throws IOException {
        List<KeyInfo> keyInfos = new ArrayList<>();

        // Create a key for each segment or every N segments (key rotation)
        int currentKeyIndex = 1;

        for (int segmentIndex = 1; segmentIndex <= estimatedSegments; segmentIndex++) {
            // Rotate to a new key every keyRotationInterval segments
            if ((segmentIndex - 1) % keyRotationInterval == 0) {
                // Create a new key and IV for the next group of segments
                String key = encryptionService.generateKeyForSegment(masterKey, currentKeyIndex);
                String iv = encryptionService.generateIVForSegment(masterIV, currentKeyIndex);

                // Create the actual key file
                String keyFilename = "key_" + currentKeyIndex + ".key";
                Path keyFilePath = resolutionDir.resolve(keyFilename);
                byte[] keyBytes = hexStringToByteArray(key);
                Files.write(keyFilePath, keyBytes);

                // Store key information
                keyInfos.add(new KeyInfo(currentKeyIndex, keyFilename, keyFilePath, key, iv));

                log.debug("Created key file {} for segments {}-{}",
                        keyFilename, segmentIndex,
                        Math.min(segmentIndex + keyRotationInterval - 1, estimatedSegments));

                currentKeyIndex++;
            }
        }

        return keyInfos;
    }

    /**
     * Create a key info file for FFmpeg
     * Format per entry: key_URI\nlocal_key_file_path\nIV_value
     */
    private void createKeyInfoFile(Path keyInfoPath, List<KeyInfo> keyInfos, String uploadId, String resolution) throws IOException {
        StringBuilder content = new StringBuilder();

        // PUBLIC URL pattern for key files
        String publicKeyBaseUrl = String.format("%s/%s/%s/", keyServerUrl, uploadId, resolution);

        for (KeyInfo keyInfo : keyInfos) {
            // Format for each entry:
            // 1. Public key URL
            // 2. Local key file path (so FFmpeg can read the key)
            // 3. IV value
            content.append(publicKeyBaseUrl).append(keyInfo.filename()).append("\n")
                    .append(keyInfo.localPath().toAbsolutePath().toString().replace("\\", "/")).append("\n")
                    .append(keyInfo.iv()).append("\n");
        }

        Files.writeString(keyInfoPath, content.toString(), StandardCharsets.UTF_8);
        log.debug("Created key info file with {} keys", keyInfos.size());
    }

    /**
     * Update URLs in the playlist (FFmpeg replaces key URIs when using a key info file)
     * But still ensure the segment URLs are correct
     */
    private void updatePlaylistUrls(Path playlistPath, String uploadId, String resolution, List<KeyInfo> keyInfos) {
        try {
            String content = Files.readString(playlistPath);

            // PUBLIC segment URL pattern
            String publicSegmentBaseUrl = String.format("%s/segments/%s/%s/", keyServerUrl, uploadId, resolution);

            // DEBUG: Log playlist content before update
            log.debug("[{}] Playlist before update (first 500 chars): {}",
                    resolution, content.substring(0, Math.min(500, content.length())));

            // Replace segment URLs (if needed)
            String segmentRegex = "seg_(\\d+)\\.ts";
            String segmentReplacement = publicSegmentBaseUrl + "seg_$1.ts";
            String updatedContent = content.replaceAll(segmentRegex, segmentReplacement);

            // Ensure the key URLs are correct (FFmpeg already replaced them when using a key info file)
            // Only verification is needed
            int keyUrlCount = countKeyUrls(updatedContent);
            log.debug("[{}] Found {} key URLs in playlist", resolution, keyUrlCount);

            Files.writeString(playlistPath, updatedContent);
            log.info("[{}] Updated playlist - Keys: {}, Segments: {}",
                    resolution, keyInfos.size(), publicSegmentBaseUrl);

        } catch (IOException e) {
            log.error("[{}] Failed to update playlist URLs", resolution, e);
            throw new RuntimeException("Failed to update playlist URLs", e);
        }
    }

    private int countKeyUrls(String content) {
        int count = 0;
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.contains("#EXT-X-KEY") && line.contains("URI=")) {
                count++;
            }
        }
        return count;
    }

    private void logGeneratedFiles(Path resolutionDir) {
        try {
            try (Stream<Path> stream = Files.list(resolutionDir)) {
                List<Path> files = stream.toList();
                log.info("[{}] Generated files:", resolutionDir.getFileName());

                Map<String, List<Path>> grouped = files.stream()
                        .collect(Collectors.groupingBy(p -> {
                            String name = p.getFileName().toString();
                            if (name.endsWith(".ts")) return "segments";
                            if (name.endsWith(".key")) return "keys";
                            if (name.endsWith(".m3u8")) return "playlists";
                            return "others";
                        }));

                for (Map.Entry<String, List<Path>> entry : grouped.entrySet()) {
                    log.info("  {}: {} files", entry.getKey(), entry.getValue().size());
                    if (entry.getKey().equals("keys")) {
                        entry.getValue().forEach(p -> {
                            try {
                                log.info("    - {} ({} bytes)", p.getFileName(), Files.size(p));
                            } catch (IOException e) {
                                log.warn("    - {} (size unknown)", p.getFileName());
                            }
                        });
                    }
                }
            }
        } catch (IOException e) {
            log.error("[{}] Failed to list files", resolutionDir, e);
        }
    }

    public void createMasterPlaylist(List<VideoResolution> resolutions, Path outputDir) {
        Path masterPath = outputDir.resolve("master.m3u8");

        StringBuilder content = new StringBuilder();
        content.append("#EXTM3U\n");
        content.append("#EXT-X-VERSION:6\n");

        for (VideoResolution res : resolutions) {
            long bandwidth = getEstimatedBandwidth(res);
            content.append(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d\n",
                    bandwidth, res.width(), res.height()));
            content.append(String.format("%s/playlist.m3u8\n", res.name()));
        }

        try {
            Files.writeString(masterPath, content.toString());
            log.info("Generated master playlist at {}", masterPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write master playlist", e);
        }
    }

    private long getEstimatedBandwidth(VideoResolution res) {
        if (res.height() >= 1080) return 6000000;
        if (res.height() >= 720) return 3000000;
        if (res.height() >= 480) return 1500000;
        return 800000;
    }

    /**
     * Record to store key information
     */
    private record KeyInfo(int index, String filename, Path localPath, String keyValue, String iv) {}
}
