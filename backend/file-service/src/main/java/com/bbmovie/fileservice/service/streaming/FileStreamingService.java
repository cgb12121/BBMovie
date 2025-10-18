package com.bbmovie.fileservice.service.streaming;

import com.bbmovie.fileservice.entity.FileAsset;
import com.bbmovie.fileservice.repository.FileAssetRepository;
import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class FileStreamingService {

    private final Cloudinary cloudinary;
    private final FileAssetRepository fileAssetRepository;

    @Value("${spring.upload-dir}")
    private String localUploadDir;

    private static final long CHUNK_SIZE = 1024 * 1024; // 1MB

    public Mono<ResponseEntity<ResourceRegion>> streamFileByMovieId(String movieId, String quality, String rangeHeader) {
        return fileAssetRepository.findByMovieIdAndQuality(movieId, quality)
                .next() // Take the first match
                .switchIfEmpty(Mono.error(new IOException("No video file found for the given movie ID and quality.")))
                .flatMap(asset -> switch (asset.getStorageProvider()) {
                    case LOCAL -> streamLocalFile(asset, rangeHeader);
                    // Cloudinary streaming with range requests requires a more complex setup or a different approach
                    // For now, this will stream the whole file, which is not ideal for video.
                    case CLOUDINARY -> streamCloudinaryFile(asset);
                    default -> Mono.error(new IOException("Unsupported storage provider."));
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    private Mono<ResponseEntity<ResourceRegion>> streamLocalFile(FileAsset asset, String rangeHeader) {
        try {
            Path filePath = Paths.get(localUploadDir, asset.getPathOrPublicId()).normalize();
            if (!filePath.startsWith(Paths.get(localUploadDir)) || !Files.exists(filePath)) {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
            }

            UrlResource videoResource = new UrlResource(filePath.toUri());
            long fileLength = videoResource.contentLength();
            ResourceRegion region = getResourceRegion(videoResource, fileLength, rangeHeader);

            return Mono.just(ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaTypeFactory.getMediaType(videoResource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .contentLength(region.getCount())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + region.getPosition() + "-" + (region.getPosition() + region.getCount() - 1) + "/" + fileLength)
                    .body(region));
        } catch (IOException e) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    private Mono<ResponseEntity<ResourceRegion>> streamCloudinaryFile(FileAsset asset) {
        // This is a placeholder. True Cloudinary range request streaming is more complex.
        // It typically involves generating a URL and letting the client handle the streaming
        // or proxying the bytes that are inefficient.
        String url = cloudinary.url().resourceType("video").publicId(asset.getPathOrPublicId()).generate();
        // This will redirect the client to the Cloudinary URL.
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, url).build());
    }

    private ResourceRegion getResourceRegion(UrlResource videoResource, long fileLength, String rangeHeader) {
        if (rangeHeader == null) {
            return new ResourceRegion(videoResource, 0, Math.min(CHUNK_SIZE, fileLength));
        }

        List<HttpRange> httpRanges = HttpRange.parseRanges(rangeHeader);
        if (httpRanges.isEmpty()) {
            return new ResourceRegion(videoResource, 0, Math.min(CHUNK_SIZE, fileLength));
        }

        HttpRange range = httpRanges.getFirst();
        long start = range.getRangeStart(fileLength);
        long end = range.getRangeEnd(fileLength);
        long length = end - start + 1;

        return new ResourceRegion(videoResource, start, length);
    }
}
