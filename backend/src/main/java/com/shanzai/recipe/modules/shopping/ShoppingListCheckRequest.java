package com.shanzai.recipe.modules.shopping;

import jakarta.validation.constraints.NotNull;

public record ShoppingListCheckRequest(
    @NotNull(message = "勾选状态不能为空")
    Boolean checked
) {
}
