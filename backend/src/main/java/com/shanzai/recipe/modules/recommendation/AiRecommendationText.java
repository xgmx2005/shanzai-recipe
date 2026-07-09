package com.shanzai.recipe.modules.recommendation;

public record AiRecommendationText(
    String summary,
    String healthTip,
    String shoppingTip,
    String topRecipeReason
) {
}
