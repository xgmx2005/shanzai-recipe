package com.shanzai.recipe.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.common.Role;
import com.shanzai.recipe.security.JwtTokenProvider;
import com.shanzai.recipe.security.JwtUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AvatarStorageService avatarStorageService;

    public AuthService(
        UserMapper userMapper,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        AvatarStorageService avatarStorageService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.avatarStorageService = avatarStorageService;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (findByUsername(request.username()) != null) {
            throw new BusinessException("用户名已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setRole(Role.USER.name());
        user.setStatus(1);
        userMapper.insert(user);
        return toLoginResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        return toLoginResponse(user);
    }

    public UserInfoResponse currentUser(JwtUser jwtUser) {
        UserEntity user = userMapper.selectById(jwtUser.userId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserInfoResponse(user);
    }

    @Transactional
    public UserInfoResponse updateCurrentUser(JwtUser jwtUser, UpdateUserRequest request) {
        UserEntity user = userMapper.selectById(jwtUser.userId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String username = request.username().trim();
        String nickname = request.nickname().trim();
        String avatarTheme = request.avatarTheme().trim();
        UserEntity sameUsernameUser = findByUsername(username);
        if (sameUsernameUser != null && !sameUsernameUser.getId().equals(user.getId())) {
            throw new BusinessException("用户名已存在");
        }

        user.setUsername(username);
        user.setNickname(nickname);
        user.setAvatarTheme(avatarTheme);
        userMapper.updateById(user);
        return toUserInfoResponse(user);
    }

    @Transactional
    public UserInfoResponse uploadAvatar(JwtUser jwtUser, org.springframework.web.multipart.MultipartFile file) {
        UserEntity user = userMapper.selectById(jwtUser.userId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String avatarUrl = avatarStorageService.store(user.getId(), file);
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);
        return toUserInfoResponse(user);
    }

    private UserEntity findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
    }

    private LoginResponse toLoginResponse(UserEntity user) {
        String token = jwtTokenProvider.generate(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                normalizeAvatarTheme(user.getAvatarTheme()),
                normalizeAvatarUrl(user.getAvatarUrl()),
                user.getRole()
        );
    }

    private UserInfoResponse toUserInfoResponse(UserEntity user) {
        return new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                normalizeAvatarTheme(user.getAvatarTheme()),
                normalizeAvatarUrl(user.getAvatarUrl()),
                user.getRole()
        );
    }

    private String normalizeAvatarTheme(String avatarTheme) {
        return avatarTheme == null || avatarTheme.isBlank() ? "leaf" : avatarTheme;
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        return avatarUrl == null ? "" : avatarUrl;
    }
}
