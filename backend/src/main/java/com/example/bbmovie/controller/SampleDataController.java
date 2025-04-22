package com.example.bbmovie.controller;

import com.example.bbmovie.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.bbmovie.service.elasticsearch.sample.SampleDataLoaderService;

import java.io.IOException;

@RestController
@RequestMapping("/api/sample-data")
@RequiredArgsConstructor
public class SampleDataController {

    private final SampleDataLoaderService sampleDataLoaderService;

    @PostMapping("/load")
    public ResponseEntity<ApiResponse<Void>> loadSampleData() {
        try {
            sampleDataLoaderService.loadSampleData();
            return ResponseEntity.ok(ApiResponse.success("Loaded data"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/clean")
    public ResponseEntity<ApiResponse<Void>> cleanSampleData() {
        try {
            sampleDataLoaderService.cleanSampleData();
            return ResponseEntity.ok(ApiResponse.success("Cleaned data"));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }
} 