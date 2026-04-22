package com.bbmovie.personalizationrecommendation.controller.openapi;

import com.bbmovie.personalizationrecommendation.dto.RecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "Personalization", description = "Recommendation endpoints")
public interface PersonalizationControllerOpenApi {
    @Operation(summary = "Get recommendations for user")
    RecommendationResponse recommendations(@PathVariable UUID userId, @RequestParam(defaultValue = "20") int limit);
}

