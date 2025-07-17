package com.example.bbmoviestream.controller;

import com.example.bbmoviestream.service.VideoStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/stream")
public class VideoStreamController {

    private final VideoStreamService videoStreamService;

    public VideoStreamController(VideoStreamService videoStreamService) {
        this.videoStreamService = videoStreamService;
    }

    @GetMapping("/{filename}")
    public Mono<ResponseEntity<Resource>> streamVideo(
            @PathVariable String filename,
            @RequestHeader HttpHeaders headers,
            Authentication authentication
    ) {
        return videoStreamService.getVideoResource(filename)
                .flatMap(resource -> {
                    try {
                        long contentLength = resource.contentLength();
                        List<HttpRange> ranges = headers.getRange();
                        
                        if (ranges.isEmpty()) {
                            // No range header, return full video
                            return Mono.just(ResponseEntity.ok()
                                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                    .contentLength(contentLength)
                                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                                    .body(resource));
                        }
                        
                        // Handle range request
                        HttpRange range = ranges.getFirst();
                        long start = range.getRangeStart(contentLength);
                        long end = range.getRangeEnd(contentLength);
                        long rangeLength = end - start + 1;
                        
                        return videoStreamService.getPartialResource(String.valueOf(resource), start, rangeLength)
                                .map(partialResource -> ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                        .contentLength(rangeLength)
                                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                                        .header(HttpHeaders.CONTENT_RANGE, 
                                                String.format("bytes %d-%d/%d", start, end, contentLength))
                                        .body(partialResource));
                        
                    } catch (IOException e) {
                        log.error("Error getting content length for video: {}", filename, e);
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                    }
                });
    }

    @GetMapping("/{filename}/hls")
    public Mono<ResponseEntity<String>> getHlsPlaylist(
            @PathVariable String filename,
            Authentication authentication
    ) {
        return videoStreamService.generateHlsPlaylist(filename)
                .map(playlist -> ResponseEntity.ok()
                        .contentType(MediaType.valueOf("application/vnd.apple.mpegurl"))
                        .body(playlist))
                .onErrorReturn(ResponseEntity.notFound().build());
    }
}