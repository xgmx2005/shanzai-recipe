package com.shanzai.recipe.modules.shopping;

import java.math.BigDecimal;

public record ShoppingListItemResponse(
    Long id,
    Long ingredientId,
    String ingredientName,
    String category,
    BigDecimal quantity,
    String unit,
    Boolean checked
) {
}
