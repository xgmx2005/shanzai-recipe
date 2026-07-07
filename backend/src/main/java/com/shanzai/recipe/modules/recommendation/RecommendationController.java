package com.shanzai.recipe.modules.recommendation;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.security.JwtUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final RecommendationHistoryService historyService;

    public RecommendationController(
        RecommendationService recommendationService,
        RecommendationHistoryService historyService
    ) {
        this.recommendationService = recommendationService;
        this.historyService = historyService;
    }

    @PostMapping
    public ApiResponse<RecommendationResponse> recommend(
        Authentication authentication,
        @Valid @RequestBody RecommendationRequest request
    ) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        return ApiResponse.ok(recommendationService.recommend(jwtUser.userId(), request));
    }

    @GetMapping("/history")
    public ApiResponse<List<RecommendationHistorySummaryResponse>> listHistories(Authentication authentication) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        return ApiResponse.ok(historyService.listHistories(jwtUser.userId()));
    }

    @GetMapping("/history/{id}")
    public ApiResponse<RecommendationHistoryDetailResponse> getHistory(
        Authentication authentication,
        @PathVariable Long id
    ) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        return ApiResponse.ok(historyService.getHistory(jwtUser.userId(), id));
    }
}
