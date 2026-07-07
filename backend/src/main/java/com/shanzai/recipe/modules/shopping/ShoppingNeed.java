package com.shanzai.recipe.modules.shopping;

import java.math.BigDecimal;

public record ShoppingNeed(
    Long ingredientId,
    String ingredientName,
    String category,
    BigDecimal quantity,
    String unit
) {
}
