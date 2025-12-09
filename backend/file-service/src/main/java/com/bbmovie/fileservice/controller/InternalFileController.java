package com.bbmovie.fileservice.controller;

import com.bbmovie.fileservice.dto.InternalUploadResponse;
import com.bbmovie.fileservice.service.internal.InternalFileService;
import com.bbmovie.fileservice.service.internal.FileConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/files")
@RequiredArgsConstructor
public class InternalFileController {

    private final InternalFileService internalFileService;
    private final FileConfirmationService fileConfirmationService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Flux<InternalUploadResponse>>> uploadInternalFiles(
            @RequestPart("files") Flux<FilePart> fileParts,
            ServerHttpRequest request
    ) {
        Flux<InternalUploadResponse> responseFlux = internalFileService.uploadGeneralFiles(fileParts)
                .map(result -> {
                    Long fileId = (Long) result.get("id");
                    String storageType = result.get("storage").toString();
                    String path = result.get("url").toString();

                    @SuppressWarnings("all")
                    String accessUrl = UriComponentsBuilder.fromHttpRequest(request)
                            .replacePath("/internal/files/download/" + fileId)
                            .toUriString();

                    return new InternalUploadResponse(fileId, storageType, path, accessUrl);
                });

        return Mono.just(ResponseEntity.ok(responseFlux));
    }

    @GetMapping("/download/{id}")
    public Mono<ResponseEntity<Resource>> downloadFile(@PathVariable Long id) {
        return internalFileService.loadFileAssetAsResource(id)
                .map(resource -> {
                    String contentDisposition = "attachment; filename=\"" + resource.getFilename() + "\"";
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                            .body(resource);
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping("/{id}/confirm")
    public Mono<ResponseEntity<Void>> confirmFile(@PathVariable Long id) {
        return fileConfirmationService.confirmFile(id)
                .thenReturn(ResponseEntity.ok().build());
    }
}
