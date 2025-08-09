package com.bbmovie.fileservice.controller;

import com.bbmovie.fileservice.service.streaming.FileStreamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/file/stream")
public class FileStreamingController {

    private final FileStreamingService streamingService;

    @Autowired
    public FileStreamingController(FileStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    // Local video as SSE: emits binary chunks as text/event-stream is not ideal; here we return bytes directly.
    @GetMapping(value = "/local/video", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> streamLocal(
            @RequestParam("name") String nameWithoutExtension,
            @RequestParam(value = "ext", required = false, defaultValue = "mp4") String extension,
            @RequestParam(value = "res", required = false) String resolution,
            @RequestParam(value = "from", required = false) Integer fromSeconds,
            @RequestParam(value = "to", required = false) Integer toSeconds
    ) {
        return streamingService.streamLocalVideo(
                nameWithoutExtension,
                extension,
                resolution,
                fromSeconds,
                toSeconds,
                DefaultDataBufferFactory.sharedInstance
        );
    }

    // Cloudinary video through privateId
    @GetMapping(value = "/cloud/video", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> streamCloudinary(
            @RequestParam("pid") String privateId,
            @RequestParam(value = "res", required = false) String resolution,
            @RequestParam(value = "from", required = false) Integer fromSeconds,
            @RequestParam(value = "to", required = false) Integer toSeconds
    ) {
        return streamingService.streamCloudinaryVideo(
                privateId,
                resolution,
                fromSeconds,
                toSeconds
        );
    }

    // Optional SSE endpoints (base64 chunks). Clients should decode base64 to bytes.
    @GetMapping(value = "/local/video/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<org.springframework.http.codec.ServerSentEvent<String>> streamLocalSse(
            @RequestParam("name") String nameWithoutExtension,
            @RequestParam(value = "ext", required = false, defaultValue = "mp4") String extension,
            @RequestParam(value = "res", required = false) String resolution,
            @RequestParam(value = "from", required = false) Integer fromSeconds,
            @RequestParam(value = "to", required = false) Integer toSeconds
    ) {
        return streamingService.streamLocalVideoSse(
                nameWithoutExtension, extension, resolution, fromSeconds, toSeconds,
                DefaultDataBufferFactory.sharedInstance
        );
    }

    @GetMapping(value = "/cloud/video/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<org.springframework.http.codec.ServerSentEvent<String>> streamCloudinarySse(
            @RequestParam("pid") String privateId,
            @RequestParam(value = "res", required = false) String resolution,
            @RequestParam(value = "from", required = false) Integer fromSeconds,
            @RequestParam(value = "to", required = false) Integer toSeconds
    ) {
        return streamingService.streamCloudinaryVideoSse(
                privateId, resolution, fromSeconds, toSeconds
        );
    }
}


