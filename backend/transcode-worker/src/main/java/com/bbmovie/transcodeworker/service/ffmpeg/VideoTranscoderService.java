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

/**
 * Service class responsible for video transcoding operations.
 * This service handles the conversion of video files into multiple resolutions (HLS format) with encryption.
 * It generates multiple quality versions of the original video, creates encrypted HLS segments,
 * and manages encryption key rotation for secure streaming.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTranscoderService {

    /** Base URL for the stream API where keys will be served */
    // Eg: http://localhost:xxxx/api/stream
    @Value("${app.transcode.key-server-url}")
    private String streamApiBaseUrl;

    /** Base URL for public HLS content in MinIO storage */
    @Value("${app.minio.public-hls-url}")
    private String minioPublicUrl;

    /** Number of segments before rotating encryption keys */
    @Value("${app.transcode.key-rotation-interval:10}")
    private int keyRotationInterval; // number of segments before rotating the key

    /** FFmpeg instance used for video transcoding operations */
    private final FFmpeg ffmpeg;
    /** Metadata service used to extract video metadata */
    private final MetadataService metadataService;
    /** Encryption service used for generating encryption keys and IVs */
    private final EncryptionService encryptionService;

    /**
     * Record class that represents a video resolution configuration for transcoding.
     *
     * @param width the target width of the resolution
     * @param height the target height of the resolution
     * @param filename the name used for the output resolution (e.g., "720p", "1080p")
     */
    public record VideoResolution(int width, int height, String filename) {
        /**
         * Returns the name of the resolution which is the same as the filename.
         *
         * @return the resolution name (e.g., "720p", "1080p")
         */
        public String name() {
            return filename; // e.g., "720p", "1080p"
        }
    }

    /**
     * Record class that defines parameters for video resolution presets.
     *
     * @param minWidth the minimum input width required to use this resolution
     * @param targetWidth the target width for the output video
     * @param targetHeight the target height for the output video
     * @param suffix the suffix used to identify the resolution (e.g., "1080p")
     */
    public record ResolutionDefinition(int minWidth, int targetWidth, int targetHeight, String suffix) { }

    /** Predefined list of resolution definitions that determine which resolutions to generate based on input video size */
    public static final List<ResolutionDefinition> PREDEFINED_RESOLUTIONS = List.of(
            new ResolutionDefinition(1920, 1920, 1080, "1080p"),
            new ResolutionDefinition(1280, 1280, 720, "720p"),
            new ResolutionDefinition(854, 854, 480, "480p"),
            new ResolutionDefinition(640, 640, 360, "360p"),
            new ResolutionDefinition(426, 426, 240, "240p"),
            new ResolutionDefinition(256, 256, 144, "144p")
    );

    /**
     * Determines the target resolutions to generate based on the metadata of the input video.
     * This method selects appropriate resolutions from the predefined list based on the input video's width,
     * or defaults to the original resolution if no suitable presets are found.
     *
     * @param meta the metadata of the input video
     * @return a list of VideoResolution objects representing the target resolutions to generate
     */
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

    /**
     * Performs the complete video transcoding process for multiple resolutions.
     * This method generates encrypted HLS segments for each target resolution with custom key rotation.
     * It creates the master playlist and manages the entire transcoding workflow.
     *
     * @param input the path to the input video file
     * @param videoResolutions the list of resolutions to generate
     * @param outputDir the directory where transcoded files should be stored
     * @param uploadId the unique identifier for the upload operation
     */
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

    /**
     * Executes the HLS transcode job for a specific video resolution.
     * This method creates the necessary directory structure, calculates encryption keys,
     * runs FFmpeg to transcode the video into HLS format, and updates the playlist URLs.
     *
     * @param executor the FFmpegExecutor to run the transcoding job
     * @param input the input video file path
     * @param res the target resolution to transcode to
     * @param outputDir the base output directory
     * @param meta the metadata of the input video
     * @param uploadId the unique identifier for the upload operation
     * @param masterKey the master encryption key
     * @param masterIV the master initialization vector
     * @throws IOException if there's an issue during the transcoding process
     */
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
        // Ensure at least 1 segment even for zero-duration or extremely short videos
        int estimatedSegments = Math.max(1, (int) Math.ceil(meta.duration() / 10.0)); // each segment is 10 seconds
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
                // THIS arg will tell FFMPEG to AUTO generate keys
                //NOTE: TEMPORARY DISABLE THIS TO PREVENT UNEXPECTED ERROR when manually creating key
//                .addExtraArgs("-hls_flags", "periodic_rekey")
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
     * Generates encryption key files for HLS segment encryption with rotation.
     * This method creates individual key files for groups of segments based on the key rotation interval.
     * Each key is derived from the master key using segment-specific derivation.
     *
     * @param resolutionDir the directory where key files should be created
     * @param masterKey the master encryption key used for key derivation
     * @param masterIV the master initialization vector used for IV derivation
     * @param estimatedSegments the estimated number of segments for the video
     * @return a list of KeyInfo objects containing information about the generated keys
     * @throws IOException if there's an issue creating the key files
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
     * Creates a key info file for FFmpeg to use during HLS encryption.
     * The file contains the public URL, local path, and IV for each encryption key.
     * Format per entry: key_URI\nlocal_key_file_path\nIV_value
     *
     * @param keyInfoPath the path where the key info file should be created
     * @param keyInfos the list of key information objects
     * @param uploadId the unique identifier for the upload operation
     * @param resolution the resolution name for this HLS stream
     * @throws IOException if there's an issue writing the key info file
     */
    private void createKeyInfoFile(Path keyInfoPath, List<KeyInfo> keyInfos, String uploadId, String resolution) throws IOException {
        StringBuilder content = new StringBuilder();

        // PUBLIC URL pattern for key files
        // streamApiBaseUrl = http://localhost:1205/api/stream
        String publicKeyBaseUrl = String.format("%s/keys/%s/%s/", streamApiBaseUrl, uploadId, resolution);

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
     * Updates URLs in the playlist file to point to the correct public locations.
     * FFmpeg replaces key URIs when using a key info file, but this method ensures
     * the segment URLs are updated to point to the public MinIO location.
     *
     * @param playlistPath the path to the playlist file to update
     * @param uploadId the unique identifier for the upload operation
     * @param resolution the resolution name for this HLS stream
     * @param keyInfos the list of key information objects
     */
    private void updatePlaylistUrls(Path playlistPath, String uploadId, String resolution, List<KeyInfo> keyInfos) {
        try {
            String content = Files.readString(playlistPath);

            // PUBLIC segment URL pattern
            // We want http://.../api/stream/segments
            String publicSegmentBaseUrl = String.format("%s/%s/%s/", minioPublicUrl, uploadId, resolution);
            // DEBUG: Log playlist content before update
            log.debug("[{}] Playlist before update (first 300 chars): {}",
                    resolution, content.substring(0, Math.min(300, content.length())));

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

    /**
     * Counts the number of key URLs in the playlist content.
     * This method parses the playlist to identify lines that contain key information.
     *
     * @param content the playlist content as a string
     * @return the number of key URLs found in the content
     */
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

    /**
     * Logs information about the generated files for a specific resolution.
     * This method provides detailed information about the types and sizes of files generated during transcoding.
     *
     * @param resolutionDir the directory containing the generated files for a specific resolution
     */
    private void logGeneratedFiles(Path resolutionDir) {
        if (!log.isTraceEnabled()) {
            return;
        }
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

    /**
     * Creates a master playlist file that references all the generated resolution playlists.
     * This HLS master playlist allows clients to select from multiple quality streams.
     *
     * @param resolutions the list of resolutions that have been generated
     * @param outputDir the output directory where the master playlist should be created
     */
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

    /**
     * Estimates the required bandwidth for a given video resolution.
     * This method provides rough bandwidth estimates based on common streaming standards.
     *
     * @param res the video resolution to estimate bandwidth for
     * @return the estimated bandwidth in bits per second
     */
    private long getEstimatedBandwidth(VideoResolution res) {
        if (res.height() >= 1080) return 6000000;
        if (res.height() >= 720) return 3000000;
        if (res.height() >= 480) return 1500000;
        return 800000;
    }

    /**
     * Record to store encryption key information for HLS segment encryption.
     *
     * @param index the sequential index of the key
     * @param filename the name of the key file
     * @param localPath the local path to the key file
     * @param keyValue the hexadecimal value of the encryption key
     * @param iv the initialization vector for encryption
     */
    private record KeyInfo(int index, String filename, Path localPath, String keyValue, String iv) {}
}
