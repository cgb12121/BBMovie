package com.bbmovie.fileservice.controller;

import com.bbmovie.fileservice.service.streaming.FileStreamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/stream")
public class StreamingController {

    private final FileStreamingService streamingService;

    @Autowired
    public StreamingController(FileStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    @GetMapping("/{movieId}")
    public Mono<ResponseEntity<ResourceRegion>> streamFile(
            @PathVariable String movieId,
            @RequestParam(value = "quality", required = false, defaultValue = "1080p") String quality,
            @RequestHeader(value = "Range", required = false) String range) {
        return streamingService.streamFileByMovieId(movieId, quality, range);
    }
}
