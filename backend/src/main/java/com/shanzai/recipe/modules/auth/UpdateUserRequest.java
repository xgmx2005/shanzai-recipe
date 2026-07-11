package com.shanzai.recipe.modules.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 50) String nickname,
        @NotBlank @Size(max = 30) String avatarTheme
) {
}
