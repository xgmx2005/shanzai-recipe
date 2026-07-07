package com.shanzai.recipe.security;

public record JwtUser(Long userId, String username, String role) {
}
