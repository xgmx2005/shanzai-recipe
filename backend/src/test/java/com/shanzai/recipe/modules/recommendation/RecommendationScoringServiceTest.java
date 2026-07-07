package com.shanzai.recipe.modules.recommendation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationScoringServiceTest {
    @Test
    void matchingIngredientsAndGoalIncreaseScore() {
        RecommendationScoringService service = new RecommendationScoringService();
        RecipeCandidate candidate = new RecipeCandidate(
            1L,
            "鸡胸肉西兰花轻食碗",
            List.of("鸡胸肉", "西兰花", "玉米"),
            List.of("鸡胸肉", "西兰花"),
            List.of("FAT_LOSS"),
            List.of("清淡", "高蛋白"),
            20,
            12
        );
        RecommendationRequestModel request = new RecommendationRequestModel(
            List.of("鸡胸肉", "西兰花"),
            List.of(),
            List.of(),
            "FAT_LOSS",
            List.of("清淡"),
            30
        );

        RecommendationScore score = service.score(candidate, request);

        assertTrue(score.eligible());
        assertTrue(score.totalScore() >= 80);
        assertTrue(score.reasons().contains("已有食材匹配度高"));
    }

    @Test
    void blockedIngredientsMakeCandidateIneligible() {
        RecommendationScoringService service = new RecommendationScoringService();
        RecipeCandidate candidate = new RecipeCandidate(
            2L,
            "花生鸡丁",
            List.of("鸡胸肉", "花生"),
            List.of("鸡胸肉", "花生"),
            List.of("BALANCED"),
            List.of("家常"),
            25,
            8
        );
        RecommendationRequestModel request = new RecommendationRequestModel(
            List.of("鸡胸肉"),
            List.of("辣椒"),
            List.of("花生"),
            "BALANCED",
            List.of("家常"),
            30
        );

        RecommendationScore score = service.score(candidate, request);

        assertFalse(score.eligible());
        assertTrue(score.reasons().contains("包含忌口或过敏食材"));
    }
}
