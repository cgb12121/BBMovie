package com.example.bbmovie.controller;

import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.service.cloudinary.CloudinaryService;
import com.example.bbmovie.service.cloudinary.CloudinaryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/cloudinary")
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    @Autowired
    public CloudinaryController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/upload/image/{movieId}")
    public ResponseEntity<ApiResponse<CloudinaryResponse>> uploadImage(
            @PathVariable Long movieId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        CloudinaryResponse response = cloudinaryService.uploadImage(movieId, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/upload/video/{movieId}")
    public ResponseEntity<ApiResponse<CloudinaryResponse>> uploadMovie(
            @PathVariable Long movieId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        CloudinaryResponse response = cloudinaryService.uploadMovie(movieId, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/upload/trailer/{movieId}")
    public ResponseEntity<ApiResponse<CloudinaryResponse>> uploadTrailer(
            @PathVariable Long movieId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        CloudinaryResponse response = cloudinaryService.uploadTrailer(movieId, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete/image/{publicId}")
    public ResponseEntity<ApiResponse<CloudinaryResponse>> deleteImage(
            @PathVariable String publicId
    ) throws IOException {
        CloudinaryResponse response = cloudinaryService.deleteImage(publicId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete/video/{publicId}")
    public ResponseEntity<ApiResponse<CloudinaryResponse>> deleteMovie(
            @PathVariable String publicId
    ) throws IOException {
        CloudinaryResponse response = cloudinaryService.deleteMovie(publicId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete/trailer/{publicId}")
    public ResponseEntity<ApiResponse<CloudinaryResponse>> deleteTrailer(
            @PathVariable String publicId
    ) throws IOException {
        CloudinaryResponse response = cloudinaryService.deleteTrailer(publicId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
