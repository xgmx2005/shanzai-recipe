package com.shanzai.recipe.modules.recommendation.conversation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConversationContextPatchRequest(
        @Size(max = 1000) String intentText,
        String dietGoal,
        List<AvailableIngredientInput> availableIngredients,
        List<String> excludedIngredients,
        List<String> allergyIngredients,
        @Min(1) Integer cookingTime,
        @Min(1) Integer servings
) {
}
