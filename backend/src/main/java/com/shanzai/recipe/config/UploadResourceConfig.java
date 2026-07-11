package com.shanzai.recipe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    private final Path avatarRoot;

    public UploadResourceConfig(@Value("${app.upload.avatar-dir:uploads/avatars}") String avatarRoot) {
        this.avatarRoot = Path.of(avatarRoot).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(toDirectoryResourceLocation(avatarRoot));
    }

    static String toDirectoryResourceLocation(Path directory) {
        String location = directory.toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
