package com.shanzai.recipe.modules.recommendation;

public record AiRecommendationAnalysis(
    String summary,
    String healthTip,
    String shoppingTip,
    String topRecipeReason,
    boolean generated
) {
}
