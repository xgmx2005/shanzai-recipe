package com.shanzai.recipe.modules.auth;

import com.shanzai.recipe.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void removesPreviousAvatarAfterReplacementSucceeds() throws Exception {
        AvatarStorageService storageService = new AvatarStorageService(tempDir.toString());
        storageService.store(7L, new MockMultipartFile(
                "file",
                "avatar.webp",
                "image/webp",
                new byte[] {1, 2, 3}
        ));

        storageService.store(7L, new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {4, 5, 6}
        ));

        assertFalse(Files.exists(tempDir.resolve("user-7").resolve("avatar.webp")));
        assertTrue(Files.exists(tempDir.resolve("user-7").resolve("avatar.png")));
    }

    @Test
    void keepsPreviousAvatarWhenReplacementFails() throws Exception {
        AvatarStorageService storageService = new AvatarStorageService(tempDir.toString());
        storageService.store(7L, new MockMultipartFile(
                "file",
                "avatar.webp",
                "image/webp",
                new byte[] {1, 2, 3}
        ));
        MultipartFile brokenFile = mock(MultipartFile.class);
        when(brokenFile.isEmpty()).thenReturn(false);
        when(brokenFile.getSize()).thenReturn(16L);
        when(brokenFile.getContentType()).thenReturn("image/png");
        when(brokenFile.getInputStream()).thenThrow(new IOException("broken stream"));

        assertThrows(BusinessException.class, () -> storageService.store(7L, brokenFile));

        assertTrue(Files.exists(tempDir.resolve("user-7").resolve("avatar.webp")));
        assertFalse(Files.exists(tempDir.resolve("user-7").resolve("avatar.png")));
    }
}
