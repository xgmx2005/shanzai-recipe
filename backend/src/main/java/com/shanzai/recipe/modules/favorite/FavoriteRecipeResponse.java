package com.shanzai.recipe.modules.favorite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FavoriteRecipeResponse(
    Long favoriteId,
    Long recipeId,
    String recipeName,
    String description,
    String imageUrl,
    Integer calories,
    BigDecimal protein,
    LocalDateTime createdAt
) {
}
