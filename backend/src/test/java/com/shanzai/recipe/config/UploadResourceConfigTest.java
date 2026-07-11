package com.shanzai.recipe.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadResourceConfigTest {
    @Test
    void avatarResourceLocationEndsWithSlashForDirectoryMapping() {
        String location = UploadResourceConfig.toDirectoryResourceLocation(Path.of("uploads", "avatars"));

        assertTrue(location.endsWith("/"));
    }
}
