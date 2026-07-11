package com.shanzai.recipe.modules.auth;

public record LoginResponse(
    String token,
    Long userId,
    String username,
    String nickname,
    String avatarTheme,
    String avatarUrl,
    String role
) {
}
