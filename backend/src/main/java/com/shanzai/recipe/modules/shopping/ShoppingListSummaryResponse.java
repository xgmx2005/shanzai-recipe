package com.shanzai.recipe.modules.shopping;

import java.time.LocalDateTime;
import java.util.List;

public record ShoppingListSummaryResponse(
    Long id,
    String title,
    List<Long> sourceRecipeIds,
    String status,
    int itemCount,
    int checkedCount,
    LocalDateTime createdAt
) {
}
