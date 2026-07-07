package com.shanzai.recipe.modules.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 50) String username,
    @NotBlank @Size(min = 6, max = 50) String password,
    @NotBlank @Size(max = 50) String nickname
) {
}
