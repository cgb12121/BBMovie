package com.bbmovie.homepagerecommendations.controller;

import com.bbmovie.homepagerecommendations.controller.openapi.HomepageControllerOpenApi;
import com.bbmovie.homepagerecommendations.dto.TrendingResponse;
import com.bbmovie.homepagerecommendations.service.TrendingAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/homepage/v1")
@RequiredArgsConstructor
@Validated
public class HomepageController implements HomepageControllerOpenApi {

    private final TrendingAggregationService trendingAggregationService;

    @GetMapping("/trending")
    public TrendingResponse trending(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        int capped = Math.min(Math.max(limit, 1), 50);
        return trendingAggregationService.topTrending(capped);
    }
}
