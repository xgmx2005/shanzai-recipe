package com.shanzai.recipe.modules.recipe;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.common.DietGoal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public ApiResponse<List<RecipeSummaryResponse>> listRecipes(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) DietGoal dietGoal,
        @RequestParam(required = false) String tag
    ) {
        return ApiResponse.ok(recipeService.listRecipes(keyword, dietGoal, tag));
    }

    @GetMapping("/{id}")
    public ApiResponse<RecipeDetailResponse> getRecipe(@PathVariable Long id) {
        return ApiResponse.ok(recipeService.getRecipeDetail(id));
    }
}
