package com.shanzai.recipe.modules.auth;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.security.JwtUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(Authentication authentication) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        return ApiResponse.ok(authService.currentUser(jwtUser));
    }

    @PatchMapping("/me")
    public ApiResponse<UserInfoResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        return ApiResponse.ok(authService.updateCurrentUser(jwtUser, request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserInfoResponse> uploadAvatar(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        return ApiResponse.ok(authService.uploadAvatar(jwtUser, file));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe(Authentication authentication) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        authService.deleteCurrentUser(jwtUser);
        return ApiResponse.ok(null);
    }
}
