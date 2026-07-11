package com.shanzai.recipe.modules.auth;

import com.shanzai.recipe.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Service
public class AvatarStorageService {
    private static final long MAX_AVATAR_BYTES = 2 * 1024 * 1024;
    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final Path avatarRoot;

    public AvatarStorageService(@Value("${app.upload.avatar-dir:uploads/avatars}") String avatarRoot) {
        this.avatarRoot = Path.of(avatarRoot).toAbsolutePath().normalize();
    }

    public String store(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException("头像图片不能超过 2MB");
        }

        String extension = SUPPORTED_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException("头像仅支持 JPG、PNG 或 WebP");
        }

        Path userDir = avatarRoot.resolve("user-" + userId).normalize();
        if (!userDir.startsWith(avatarRoot)) {
            throw new BusinessException("头像保存路径不合法");
        }

        try {
            Files.createDirectories(userDir);
            clearOldAvatars(userDir);
            Path target = userDir.resolve("avatar." + extension);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/avatars/user-" + userId + "/avatar." + extension;
        } catch (IOException exception) {
            throw new BusinessException("头像保存失败，请稍后重试");
        }
    }

    private void clearOldAvatars(Path userDir) throws IOException {
        try (var files = Files.list(userDir)) {
            files
                    .filter(path -> path.getFileName().toString().startsWith("avatar."))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // A stale old avatar should not block replacing it with the new upload.
                        }
                    });
        }
    }
}
