package com.shanzai.recipe.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenProviderTest {
    @Test
    void generatedTokenCanBeParsed() {
        JwtTokenProvider provider = new JwtTokenProvider(
            "change-this-secret-to-at-least-32-characters",
            1440
        );

        String token = provider.generate(1L, "user1", "USER");
        JwtUser user = provider.parse(token);

        assertEquals(1L, user.userId());
        assertEquals("user1", user.username());
        assertEquals("USER", user.role());
    }
}
