package com.shanzai.recipe.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.modules.favorite.FavoriteEntity;
import com.shanzai.recipe.modules.favorite.FavoriteMapper;
import com.shanzai.recipe.modules.profile.ProfileEntity;
import com.shanzai.recipe.modules.profile.ProfileMapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.common.Role;
import com.shanzai.recipe.security.JwtTokenProvider;
import com.shanzai.recipe.security.JwtUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final AvatarStorageService avatarStorageService = mock(AvatarStorageService.class);
    private final ProfileMapper profileMapper = mock(ProfileMapper.class);
    private final FavoriteMapper favoriteMapper = mock(FavoriteMapper.class);
    private final AuthService authService = new AuthService(
            userMapper,
            passwordEncoder,
            jwtTokenProvider,
            avatarStorageService,
            profileMapper,
            favoriteMapper
    );

    @Test
    void updateCurrentUserChangesUsernameAndNickname() {
        UserEntity existing = user(1L, "user1", "体验用户");
        when(userMapper.selectById(1L)).thenReturn(existing);
        when(userMapper.selectOne(anyUserQuery())).thenReturn(null);

        UserInfoResponse updated = authService.updateCurrentUser(
                new JwtUser(1L, "user1", Role.USER.name()),
                new UpdateUserRequest("newUser", "新的昵称", "tomato")
        );

        assertEquals("newUser", updated.username());
        assertEquals("新的昵称", updated.nickname());
        assertEquals("tomato", updated.avatarTheme());
        assertEquals("", updated.avatarUrl());
        verify(userMapper).updateById(existing);
    }

    @Test
    void uploadAvatarStoresFileAndUpdatesCurrentUser() {
        UserEntity existing = user(1L, "user1", "体验用户");
        MultipartFile file = mock(MultipartFile.class);
        when(userMapper.selectById(1L)).thenReturn(existing);
        when(avatarStorageService.store(1L, file)).thenReturn("/uploads/avatars/user-1/avatar.webp");

        UserInfoResponse updated = authService.uploadAvatar(
                new JwtUser(1L, "user1", Role.USER.name()),
                file
        );

        assertEquals("/uploads/avatars/user-1/avatar.webp", updated.avatarUrl());
        assertEquals("/uploads/avatars/user-1/avatar.webp", existing.getAvatarUrl());
        verify(userMapper).updateById(existing);
    }

    @Test
    void updateCurrentUserRejectsDuplicateUsername() {
        UserEntity existing = user(1L, "user1", "体验用户");
        when(userMapper.selectById(1L)).thenReturn(existing);
        when(userMapper.selectOne(anyUserQuery())).thenReturn(user(2L, "newUser", "别人"));

        assertThrows(BusinessException.class, () -> authService.updateCurrentUser(
                new JwtUser(1L, "user1", Role.USER.name()),
                new UpdateUserRequest("newUser", "新的昵称", "tomato")
        ));
        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    @Test
    void deleteCurrentUserAnonymizesAccountAndClearsPrivateData() {
        UserEntity existing = user(1L, "user1", "体验用户");
        existing.setAvatarUrl("/uploads/avatars/user-1/avatar.webp");
        ProfileEntity profile = profile(1L);
        when(userMapper.selectById(1L)).thenReturn(existing);
        when(profileMapper.selectOne(anyProfileQuery())).thenReturn(profile);

        authService.deleteCurrentUser(new JwtUser(1L, "user1", Role.USER.name()));

        assertEquals(0, existing.getStatus());
        assertTrue(existing.getUsername().startsWith("deleted_1_"));
        assertEquals("已注销用户", existing.getNickname());
        assertEquals("", existing.getAvatarUrl());
        assertEquals("leaf", existing.getAvatarTheme());
        assertEquals(null, profile.getGender());
        assertEquals(null, profile.getAge());
        assertEquals(null, profile.getHeightCm());
        assertEquals(null, profile.getWeightKg());
        assertEquals(null, profile.getBmi());
        assertEquals("BALANCED", profile.getDietGoal());
        assertEquals(null, profile.getTastePreferences());
        assertEquals(null, profile.getAvoidIngredients());
        assertEquals(null, profile.getAllergyIngredients());
        assertEquals(null, profile.getCookingTimePreference());
        assertEquals(null, profile.getDailyCalorieTarget());
        assertEquals(false, profile.getProfileCompleted());
        verify(avatarStorageService).deleteAvatar(1L);
        verify(profileMapper).updateById(profile);
        verify(favoriteMapper).delete(anyFavoriteQuery());
        verify(userMapper).updateById(existing);
    }

    @Test
    void deleteCurrentUserRejectsMaintainerAccount() {
        UserEntity maintainer = user(2L, "maintainer", "维护员");
        maintainer.setRole(Role.MAINTAINER.name());
        when(userMapper.selectById(2L)).thenReturn(maintainer);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.deleteCurrentUser(
                new JwtUser(2L, "maintainer", Role.MAINTAINER.name())
        ));

        assertEquals("管理员账号不能在前台注销", exception.getMessage());
        verify(userMapper, never()).updateById(any(UserEntity.class));
        verify(avatarStorageService, never()).deleteAvatar(any());
        verify(profileMapper, never()).updateById(any(ProfileEntity.class));
        verify(favoriteMapper, never()).delete(anyFavoriteQuery());
    }

    @Test
    void currentUserRejectsDisabledAccount() {
        UserEntity disabled = user(1L, "user1", "体验用户");
        disabled.setStatus(0);
        when(userMapper.selectById(1L)).thenReturn(disabled);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.currentUser(
                new JwtUser(1L, "user1", Role.USER.name())
        ));

        assertEquals("账号已被禁用", exception.getMessage());
    }

    private static UserEntity user(Long id, String username, String nickname) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setAvatarTheme("leaf");
        user.setAvatarUrl("");
        user.setRole(Role.USER.name());
        user.setStatus(1);
        return user;
    }

    private static ProfileEntity profile(Long userId) {
        ProfileEntity profile = new ProfileEntity();
        profile.setId(10L);
        profile.setUserId(userId);
        profile.setGender("FEMALE");
        profile.setAge(20);
        profile.setHeightCm(new BigDecimal("165.00"));
        profile.setWeightKg(new BigDecimal("55.00"));
        profile.setBmi(new BigDecimal("20.20"));
        profile.setDietGoal("FAT_LOSS");
        profile.setTastePreferences("清淡,高蛋白");
        profile.setAvoidIngredients("辣椒");
        profile.setAllergyIngredients("花生");
        profile.setCookingTimePreference(30);
        profile.setDailyCalorieTarget(1600);
        profile.setProfileCompleted(true);
        return profile;
    }

    @SuppressWarnings("unchecked")
    private static LambdaQueryWrapper<UserEntity> anyUserQuery() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private static LambdaQueryWrapper<ProfileEntity> anyProfileQuery() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private static LambdaQueryWrapper<FavoriteEntity> anyFavoriteQuery() {
        return any(LambdaQueryWrapper.class);
    }
}
