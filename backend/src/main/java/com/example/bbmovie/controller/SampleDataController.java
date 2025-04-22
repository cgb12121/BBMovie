package com.example.bbmovie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.bbmovie.service.elasticsearch.sample.SampleDataLoaderService;

@RestController
@RequestMapping("/api/sample-data")
@RequiredArgsConstructor
public class SampleDataController {

    private final SampleDataLoaderService sampleDataLoaderService;

    @PostMapping("/load")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> loadSampleData() {
        sampleDataLoaderService.loadSampleData();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clean")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cleanSampleData() {
        sampleDataLoaderService.cleanSampleData();
        return ResponseEntity.ok().build();
    }
} 