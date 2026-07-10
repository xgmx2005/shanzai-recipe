package com.shanzai.recipe.modules.recommendation.conversation;

import java.math.BigDecimal;

public record AvailableIngredientInput(
        String name,
        BigDecimal quantity,
        String unit,
        boolean quantityKnown
) {
}
