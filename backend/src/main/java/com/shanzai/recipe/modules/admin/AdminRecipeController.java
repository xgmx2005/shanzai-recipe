package com.shanzai.recipe.modules.admin;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.common.DietGoal;
import com.shanzai.recipe.modules.recipe.RecipeDetailResponse;
import com.shanzai.recipe.modules.recipe.RecipeSaveRequest;
import com.shanzai.recipe.modules.recipe.RecipeService;
import com.shanzai.recipe.modules.recipe.RecipeSummaryResponse;
import com.shanzai.recipe.security.JwtUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recipes")
public class AdminRecipeController {
    private final RecipeService recipeService;

    public AdminRecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public ApiResponse<List<RecipeSummaryResponse>> listRecipes(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) DietGoal dietGoal,
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.ok(recipeService.listAdminRecipes(keyword, dietGoal, tag, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<RecipeDetailResponse> getRecipe(@PathVariable Long id) {
        return ApiResponse.ok(recipeService.getAdminRecipeDetail(id));
    }

    @PostMapping
    public ApiResponse<RecipeDetailResponse> createRecipe(
        Authentication authentication,
        @Valid @RequestBody RecipeSaveRequest request
    ) {
        return ApiResponse.ok(recipeService.createRecipe(currentUserId(authentication), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RecipeDetailResponse> updateRecipe(
        @PathVariable Long id,
        @Valid @RequestBody RecipeSaveRequest request
    ) {
        return ApiResponse.ok(recipeService.updateRecipe(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ApiResponse.ok(null);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof JwtUser jwtUser) {
            return jwtUser.userId();
        }
        return null;
    }
}
