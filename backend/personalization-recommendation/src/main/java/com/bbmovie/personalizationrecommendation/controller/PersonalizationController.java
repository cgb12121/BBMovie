package com.bbmovie.personalizationrecommendation.controller;

import com.bbmovie.personalizationrecommendation.controller.openapi.PersonalizationControllerOpenApi;
import com.bbmovie.personalizationrecommendation.dto.RecommendationResponse;
import com.bbmovie.personalizationrecommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/personalization/v1/users")
public class PersonalizationController implements PersonalizationControllerOpenApi {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}/recommendations")
    public RecommendationResponse recommendations(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return recommendationService.recommend(userId, limit);
    }
}

