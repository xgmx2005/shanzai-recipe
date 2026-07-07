package com.shanzai.recipe.modules.profile;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.security.JwtUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<ProfileResponse> getProfile(Authentication authentication) {
        return ApiResponse.ok(profileService.getProfile(currentUserId(authentication)));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> saveProfile(
        Authentication authentication,
        @Valid @RequestBody ProfileRequest request
    ) {
        return ApiResponse.ok(profileService.saveProfile(currentUserId(authentication), request));
    }

    @GetMapping("/summary")
    public ApiResponse<ProfileSummaryResponse> getSummary(Authentication authentication) {
        return ApiResponse.ok(profileService.getSummary(currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        return jwtUser.userId();
    }
}
