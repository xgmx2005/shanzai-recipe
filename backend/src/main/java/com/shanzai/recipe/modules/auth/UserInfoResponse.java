package com.shanzai.recipe.modules.auth;

public record UserInfoResponse(
    Long userId,
    String username,
    String nickname,
    String role
) {
}
