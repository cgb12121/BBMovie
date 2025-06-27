package com.example.bbmovieuploadfile.controller;

import com.example.bbmovieuploadfile.serive.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController("file")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/upload")
    public Mono<ResponseEntity<String>> handleUpload(@RequestPart("file") FilePart filePart, Authentication auth) {
        return fileUploadService.uploadFile(filePart, auth);
    }
}
