package com.bbmovie.homepagerecommendations.controller.openapi;

import com.bbmovie.homepagerecommendations.dto.TrendingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

@SuppressWarnings("unused")
@Tag(name = "Homepage", description = "Homepage recommendation endpoints")
public interface HomepageControllerOpenApi {
    @Operation(summary = "Get trending items", description = "Returns top trending items with bounded limit")
    TrendingResponse trending(@RequestParam(name = "limit", defaultValue = "10") int limit);
}

