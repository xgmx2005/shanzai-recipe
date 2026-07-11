package com.shanzai.recipe.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.common.Role;
import com.shanzai.recipe.security.JwtTokenProvider;
import com.shanzai.recipe.security.JwtUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final AuthService authService = new AuthService(userMapper, passwordEncoder, jwtTokenProvider);

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

    private static UserEntity user(Long id, String username, String nickname) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setAvatarTheme("leaf");
        user.setRole(Role.USER.name());
        user.setStatus(1);
        return user;
    }

    @SuppressWarnings("unchecked")
    private static LambdaQueryWrapper<UserEntity> anyUserQuery() {
        return any(LambdaQueryWrapper.class);
    }
}
