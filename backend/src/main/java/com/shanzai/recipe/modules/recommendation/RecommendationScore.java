package com.shanzai.recipe.modules.recommendation;

import java.util.List;

public record RecommendationScore(
    int totalScore,
    List<String> reasons,
    boolean eligible
) {
    public static RecommendationScore ineligible(String reason) {
        return new RecommendationScore(0, List.of(reason), false);
    }
}
