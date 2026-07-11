package com.shanzai.recipe.modules.auth;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.security.JwtUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
