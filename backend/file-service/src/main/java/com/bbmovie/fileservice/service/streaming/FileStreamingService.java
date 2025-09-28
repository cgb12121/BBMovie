package com.bbmovie.fileservice.service.streaming;

import com.bbmovie.fileservice.constraints.ResolutionConstraints;
import com.bbmovie.fileservice.service.ffmpeg.FFmpegVideoMetadata;
import com.bbmovie.fileservice.service.ffmpeg.ImageExtension;
import com.bbmovie.fileservice.service.ffmpeg.VideoMetadataService;
import com.bbmovie.fileservice.utils.PrivateIdCodec;
import com.cloudinary.AuthToken;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Log4j2
@Service
@RequiredArgsConstructor
public class FileStreamingService {

    private final Cloudinary cloudinary;
    private final VideoMetadataService videoMetadataService;
    private final WebClient webClient = WebClient.builder().build();

    @Value("${spring.upload-dir}")
    private String localUploadDir;

    @Value("${file.private-id.secret:change-me}")
    private String privateIdSecret;

    // Default 10 minutes in seconds
    private static final int DEFAULT_WINDOW_SECONDS = 10 * 60;

    AuthToken token = new AuthToken()
            .duration(600) // seconds
            .startTime(System.currentTimeMillis() / 1000L);

    private static final MediaType MEDIA_TYPE_IMAGE_WEBP = MediaType.valueOf("image/webp");
    private static final MediaType MEDIA_TYPE_IMAGE_BMP = MediaType.valueOf("image/bmp");

    public Mono<ResponseEntity<Flux<DataBuffer>>> streamLocalVideo(
            String baseName, String extension, String resolution,
            Integer fromSeconds, Integer toSeconds, DataBufferFactory bufferFactory
    ) {
        String safeResolution = resolveResolutionSuffix(resolution);
        String fileName = buildLocalFileName(baseName, extension, safeResolution);
        Path filePath = Paths.get(localUploadDir, fileName).normalize();

        // Security & existence check
        if (!filePath.startsWith(Paths.get(localUploadDir))) {
            return Mono.error(new SecurityException("Invalid file path"));
        }
        if (!Files.exists(filePath)) {
            return Mono.error(new IllegalArgumentException("File not found"));
        }

        return videoMetadataService.getMetadata(filePath)
                .flatMap(metadata -> {
                    log.info("Metadata: {}", metadata);
                    return createFileStreamingResponse(filePath, metadata, fromSeconds, toSeconds, bufferFactory);
                });
    }

    public Mono<ResponseEntity<Flux<DataBuffer>>> streamLocalImage(
            String baseName,
            String extension,
            DataBufferFactory bufferFactory
    ) {
        return Mono.fromCallable(() -> resolveLocalImagePath(baseName, extension))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(resolved -> Mono.fromCallable(() -> validateLocalImagePath(resolved.path()))
                        .flatMap(contentLength -> {
                            MediaType mediaType = resolveImageMediaType(resolved.extension(), resolved.path().getFileName().toString());
                            Flux<DataBuffer> body = DataBufferUtils.read(resolved.path(), bufferFactory, 16 * 1024);
                            return Mono.just(ResponseEntity.ok()
                                    .contentType(mediaType)
                                    .contentLength(contentLength)
                                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                                    .body(body));
                        }));
    }

    private long validateLocalImagePath(Path filePath) throws IOException {
        if (!filePath.startsWith(Paths.get(localUploadDir))) {
            throw new SecurityException("Invalid file path");
        }
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Image not found");
        }
        return Files.size(filePath);
    }

    private Mono<ResponseEntity<Flux<DataBuffer>>> createFileStreamingResponse(
            Path filePath, FFmpegVideoMetadata meta, Integer fromSeconds,
            Integer toSeconds, DataBufferFactory bufferFactory
    ) {
        return Mono.fromCallable(() -> Files.size(filePath))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(fileSize -> {
                    double duration = Math.max(1.0, meta.duration() * 60d);

                    int startSec = Math.clamp(fromSeconds, 0, (int) duration);
                    int endSec = Math.clamp(toSeconds, startSec + 1, (int) duration + DEFAULT_WINDOW_SECONDS);
                    endSec = Math.max(startSec + 1, endSec); // ensure end > start

                    long startByte = (long) ((startSec / duration) * fileSize);
                    long endByte = (long) ((endSec / duration) * fileSize);

                    startByte = Math.clamp(startByte, 0L, fileSize);
                    endByte = Math.clamp(endByte, startByte, fileSize);
                    long bytesToRead = endByte - startByte;

                    int bufferSize = 1024 * 128;
                    AtomicLong readSoFar = new AtomicLong(0);

                    long finalStartByte = startByte;
                    Flux<DataBuffer> flux = DataBufferUtils.readInputStream(
                                    () -> newInputStreamWithOffset(filePath, finalStartByte),
                                    bufferFactory,
                                    bufferSize
                            )
                            .takeWhile(dataBuffer -> readSoFar.addAndGet(dataBuffer.readableByteCount()) <= bytesToRead);

                    return Mono.just(ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .body(flux));
                });
    }

    @SuppressWarnings("squid:S2095")
    private InputStream newInputStreamWithOffset(Path path, long offset) throws IOException {
        InputStream in = Files.newInputStream(path, StandardOpenOption.READ);
        long skipped = 0;
        while (skipped < offset) {
            long n = in.skip(offset - skipped);
            if (n <= 0) break;
            skipped += n;
        }
        return in;
    }

    public Mono<ResponseEntity<Flux<DataBuffer>>> streamCloudinaryVideo(
            String privateId,
            String resolution,
            Integer fromSeconds,
            Integer toSeconds
    ) {
        String publicId = PrivateIdCodec.decode(privateId, privateIdSecret);
        Transformation<?> transformation = buildTransformation(resolution, fromSeconds, toSeconds);

        String signedUrl = cloudinary.url()
                .secure(true)
                .type("authenticated")
                .publicId(publicId)
                .authToken(token)
                .transformation(transformation)
                .generate();

        Flux<DataBuffer> body = webClient.get()
                .uri(signedUrl)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .timeout(Duration.ofMinutes(30));

        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(body));
    }

    public Mono<ResponseEntity<Flux<DataBuffer>>> streamCloudinaryImage(
            String privateId,
            Integer width,
            Integer height,
            String format
    ) {
        String publicId = PrivateIdCodec.decode(privateId, privateIdSecret);

        Transformation<?> transformation = new Transformation<>();
        if (width != null) {
            if (width <= 0) {
                return Mono.error(new IllegalArgumentException("Width must be positive"));
            }
            transformation.width(width);
        }
        if (height != null) {
            if (height <= 0) {
                return Mono.error(new IllegalArgumentException("Height must be positive"));
            }
            transformation.height(height);
        }
        if ((width != null && width > 0) || (height != null && height > 0)) {
            transformation.crop("fill");
        }

        String normalizedFormat = normalizeImageExtension(format, null);
        if (normalizedFormat != null) {
            transformation.fetchFormat(normalizedFormat);
        }

        String signedUrl = cloudinary.url()
                .secure(true)
                .type("authenticated")
                .resourceType("image")
                .publicId(publicId)
                .authToken(token)
                .transformation(transformation)
                .generate();

        return webClient.get()
                .uri(signedUrl)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.createException().flatMap(Mono::error);
                    }

                    MediaType mediaType = response.headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
                    long contentLength = response.headers().asHttpHeaders().getContentLength();

                    Flux<DataBuffer> body = response.bodyToFlux(DataBuffer.class)
                            .timeout(Duration.ofMinutes(5));

                    ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                            .contentType(mediaType);
                    if (contentLength >= 0) {
                        builder.contentLength(contentLength);
                    }
                    return Mono.just(builder.body(body));
                });
    }

    public Flux<ServerSentEvent<String>> streamLocalVideoSse(
            String baseName, String extension, String resolution,
            Integer fromSeconds, Integer toSeconds, DataBufferFactory bufferFactory
    ) {
        return streamLocalVideo(baseName, extension, resolution, fromSeconds, toSeconds, bufferFactory)
                .flatMapMany(response -> Optional.ofNullable(response.getBody())
                        .map(Flux::from)
                        .orElse(Flux.empty()))
                .map(this::encodeToSse);
    }

    public Flux<ServerSentEvent<String>> streamCloudinaryVideoSse(
            String privateId, String resolution, Integer fromSeconds, Integer toSeconds
    ) {
        return streamCloudinaryVideo(privateId, resolution, fromSeconds, toSeconds)
                .flatMapMany(response -> Optional.ofNullable(response.getBody())
                        .map(Flux::from)
                        .orElse(Flux.empty()))
                .map(this::encodeToSse);
    }

    private ServerSentEvent<String> encodeToSse(DataBuffer buffer) {
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        DataBufferUtils.release(buffer); // prevent memory leaks
        String b64 = Base64.getEncoder().encodeToString(bytes);
        return ServerSentEvent.<String>builder().data(b64).build();
    }

    private String buildLocalFileName(String baseName, String extension, String resolution) {
        String sanitizedBase = sanitizeBase(baseName);
        String ext = StringUtils.hasText(extension) ? extension : "mp4";
        return StringUtils.hasText(resolution)
                ? sanitizedBase + "_" + resolution + "." + ext
                : sanitizedBase + "." + ext;
    }

    private String buildLocalImageFileName(String baseName, String extension) {
        String sanitizedBase = sanitizeBase(baseName);
        return sanitizedBase + "." + extension;
    }

    private record ResolvedLocalImage(Path path, String extension) {}

    private ResolvedLocalImage resolveLocalImagePath(String baseName, String extension) {
        String sanitizedBase = sanitizeBase(baseName);
        List<String> candidates;
        if (StringUtils.hasText(extension)) {
            candidates = List.of(normalizeImageExtension(extension, null));
        } else {
            candidates = ImageExtension.getAllowedExtensions().stream()
                    .map(ImageExtension::getExtension)
                    .toList();
        }

        Path uploadRoot = Paths.get(localUploadDir);
        for (String candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            Path filePath = uploadRoot.resolve(buildLocalImageFileName(sanitizedBase, candidate)).normalize();
            if (Files.exists(filePath)) {
                return new ResolvedLocalImage(filePath, candidate);
            }
        }

        throw new IllegalArgumentException("Image not found");
    }

    private String resolveResolutionSuffix(String desired) {
        if (!StringUtils.hasText(desired)) return null;
        List<String> allowed = List.of(
                ResolutionConstraints._1080P,
                ResolutionConstraints._720P,
                ResolutionConstraints._480P,
                ResolutionConstraints._360P,
                ResolutionConstraints._240P,
                ResolutionConstraints._144P
        );
        return allowed.stream()
                .filter(a -> a.equalsIgnoreCase(desired))
                .findFirst()
                .orElse(null);
    }

    private String sanitizeBase(String base) {
        return FilenameUtils.getBaseName(base)
                .replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private static final Map<String, String> IMAGE_EXTENSION_ALIASES = createImageExtensionAliases();

    private static Map<String, String> createImageExtensionAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("jpeg", "jpg");
        return aliases;
    }

    private String normalizeImageExtension(String extension, String defaultExtension) {
        if (!StringUtils.hasText(extension)) {
            return defaultExtension;
        }

        String normalized = extension.toLowerCase();
        normalized = IMAGE_EXTENSION_ALIASES.getOrDefault(normalized, normalized);
        boolean allowed = ImageExtension.getAllowedExtensions().stream()
                .map(ImageExtension::getExtension)
                .anyMatch(normalized::equals);

        if (!allowed) {
            throw new IllegalArgumentException("Unsupported image extension: " + extension);
        }

        return normalized;
    }

    private MediaType resolveImageMediaType(String extension, String fileName) {
        return MediaTypeFactory.getMediaType(fileName)
                .orElseGet(() -> switch (extension) {
                    case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                    case "png" -> MediaType.IMAGE_PNG;
                    case "webp" -> MEDIA_TYPE_IMAGE_WEBP;
                    case "bmp" -> MEDIA_TYPE_IMAGE_BMP;
                    default -> MediaType.APPLICATION_OCTET_STREAM;
                });
    }

    private Transformation<?> buildTransformation(String resolution, Integer from, Integer to) {
        Transformation<?> t = new Transformation<>();
        String res = resolveResolutionSuffix(resolution);

        switch (res) {
            case ResolutionConstraints._1080P -> t.width(1920).height(1080).crop("fill");
            case ResolutionConstraints._720P -> t.width(1280).height(720).crop("fill");
            case ResolutionConstraints._480P -> t.width(854).height(480).crop("fill");
            case ResolutionConstraints._360P -> t.width(640).height(360).crop("fill");
            case ResolutionConstraints._240P -> t.width(320).height(240).crop("fill");
            case ResolutionConstraints._144P -> t.width(160).height(144).crop("fill");
            case null ->  throw new IllegalArgumentException("Invalid resolution");
            default -> { /* keep the original resolution */ }
        }

        int start = Math.clamp(from, 0, Integer.MAX_VALUE);
        int end = Math.clamp(to, start + 1, start + DEFAULT_WINDOW_SECONDS);
        t.startOffset(start).endOffset(Math.max(start + 1, end));
        return t;
    }
}


