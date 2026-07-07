package com.shanzai.recipe.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {
    @Test
    void successResponseCarriesData() {
        ApiResponse<String> response = ApiResponse.ok("ready");

        assertTrue(response.success());
        assertEquals("OK", response.message());
        assertEquals("ready", response.data());
    }

    @Test
    void failureResponseCarriesMessageWithoutData() {
        ApiResponse<String> response = ApiResponse.fail("参数错误");

        assertFalse(response.success());
        assertEquals("参数错误", response.message());
        assertNull(response.data());
    }
}
