package com.shanzai.recipe.modules.favorite;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.security.JwtUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/recipes/{id}/favorite")
    public ApiResponse<FavoriteRecipeResponse> favoriteRecipe(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(favoriteService.favoriteRecipe(currentUserId(authentication), id));
    }

    @DeleteMapping("/recipes/{id}/favorite")
    public ApiResponse<Void> unfavoriteRecipe(Authentication authentication, @PathVariable Long id) {
        favoriteService.unfavoriteRecipe(currentUserId(authentication), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/favorites")
    public ApiResponse<List<FavoriteRecipeResponse>> listFavorites(Authentication authentication) {
        return ApiResponse.ok(favoriteService.listFavorites(currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        return jwtUser.userId();
    }
}
