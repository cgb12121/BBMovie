package com.bbmovie.fileservice.controller;

import com.bbmovie.fileservice.dto.CloudinaryUsageRequest;
import com.bbmovie.fileservice.dto.DiskUsageResponse;
import com.bbmovie.fileservice.entity.FileAsset;
import com.bbmovie.fileservice.service.admin.AdminService;
import com.cloudinary.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/files")
    public Flux<FileAsset> listAllFiles(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return adminService.listAllFiles(pageable);
    }

    @DeleteMapping("/files/{id}")
    public Mono<ResponseEntity<Void>> deleteFile(@PathVariable Long id) {
        return adminService.deleteFileAsset(id).then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping("/cloudinary/usage")
    public Mono<ApiResponse> cloudinaryUsage(@RequestBody CloudinaryUsageRequest request) {
        return adminService.cloudinaryUsage(request);
    }

    @GetMapping("/disk-usage")
    public Mono<DiskUsageResponse> getDiskUsage() {
        return adminService.getDiskUsage();
    }

    @GetMapping("/cloudinary/resources")
    public Mono<ApiResponse> listCloudinaryResources() {
        return adminService.listCloudinaryResources();
    }

    @DeleteMapping("/cloudinary/resources")
    public Mono<ApiResponse> deleteCloudinaryResource(@RequestParam("publicId") String publicId) {
        return adminService.deleteCloudinaryResource(publicId);
    }
}
