package com.shanzai.recipe.modules.auth;

import com.shanzai.recipe.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void storesWebpAvatarUnderUserScopedUploadsPath() throws Exception {
        AvatarStorageService storageService = new AvatarStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.webp",
                "image/webp",
                new byte[] {1, 2, 3}
        );

        String avatarUrl = storageService.store(7L, file);

        assertEquals("/uploads/avatars/user-7/avatar.webp", avatarUrl);
        assertTrue(Files.exists(tempDir.resolve("user-7").resolve("avatar.webp")));
    }

    @Test
    void rejectsNonImageAvatarFile() {
        AvatarStorageService storageService = new AvatarStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.txt",
                "text/plain",
                "not-image".getBytes()
        );

        assertThrows(BusinessException.class, () -> storageService.store(7L, file));
    }
}
