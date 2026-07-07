package com.shanzai.recipe.modules.shopping;

import java.time.LocalDateTime;
import java.util.List;

public record ShoppingListResponse(
    Long id,
    String title,
    List<Long> sourceRecipeIds,
    String status,
    List<ShoppingListItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
