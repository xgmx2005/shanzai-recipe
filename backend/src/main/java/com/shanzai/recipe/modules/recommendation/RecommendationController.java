package com.shanzai.recipe.modules.recommendation;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.security.JwtUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ApiResponse<RecommendationResponse> recommend(
        Authentication authentication,
        @Valid @RequestBody RecommendationRequest request
    ) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        return ApiResponse.ok(recommendationService.recommend(jwtUser.userId(), request));
    }
}
