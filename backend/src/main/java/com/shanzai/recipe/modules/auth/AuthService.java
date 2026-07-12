package com.shanzai.recipe.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.common.Role;
import com.shanzai.recipe.modules.favorite.FavoriteEntity;
import com.shanzai.recipe.modules.favorite.FavoriteMapper;
import com.shanzai.recipe.modules.profile.ProfileEntity;
import com.shanzai.recipe.modules.profile.ProfileMapper;
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
    private final ProfileMapper profileMapper;
    private final FavoriteMapper favoriteMapper;

    public AuthService(
        UserMapper userMapper,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        AvatarStorageService avatarStorageService,
        ProfileMapper profileMapper,
        FavoriteMapper favoriteMapper
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.avatarStorageService = avatarStorageService;
        this.profileMapper = profileMapper;
        this.favoriteMapper = favoriteMapper;
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
        UserEntity user = activeUser(jwtUser.userId());
        return toUserInfoResponse(user);
    }

    @Transactional
    public UserInfoResponse updateCurrentUser(JwtUser jwtUser, UpdateUserRequest request) {
        UserEntity user = activeUser(jwtUser.userId());

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
        UserEntity user = activeUser(jwtUser.userId());

        String avatarUrl = avatarStorageService.store(user.getId(), file);
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);
        return toUserInfoResponse(user);
    }

    @Transactional
    public void deleteCurrentUser(JwtUser jwtUser) {
        UserEntity user = activeUser(jwtUser.userId());
        if (Role.MAINTAINER.name().equals(user.getRole())) {
            throw new BusinessException("管理员账号不能在前台注销");
        }

        avatarStorageService.deleteAvatar(user.getId());
        clearProfile(user.getId());
        favoriteMapper.delete(new LambdaQueryWrapper<FavoriteEntity>().eq(FavoriteEntity::getUserId, user.getId()));

        user.setStatus(0);
        user.setUsername("deleted_" + user.getId() + "_" + System.currentTimeMillis());
        user.setNickname("已注销用户");
        user.setAvatarUrl("");
        user.setAvatarTheme("leaf");
        userMapper.updateById(user);
    }

    private UserEntity activeUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        return user;
    }

    private void clearProfile(Long userId) {
        ProfileEntity profile = profileMapper.selectOne(
                new LambdaQueryWrapper<ProfileEntity>().eq(ProfileEntity::getUserId, userId)
        );
        if (profile == null) {
            return;
        }
        profile.setGender(null);
        profile.setAge(null);
        profile.setHeightCm(null);
        profile.setWeightKg(null);
        profile.setBmi(null);
        profile.setDietGoal("BALANCED");
        profile.setTastePreferences(null);
        profile.setAvoidIngredients(null);
        profile.setAllergyIngredients(null);
        profile.setCookingTimePreference(null);
        profile.setDailyCalorieTarget(null);
        profile.setProfileCompleted(false);
        profileMapper.updateById(profile);
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
