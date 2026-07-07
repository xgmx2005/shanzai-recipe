package com.shanzai.recipe.modules.shopping;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ShoppingListCreateRequest(
    @NotEmpty(message = "请选择菜谱")
    List<Long> recipeIds,
    List<String> availableIngredients,
    String title
) {
}
