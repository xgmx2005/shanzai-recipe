package com.shanzai.recipe.modules.admin;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.modules.ingredient.IngredientResponse;
import com.shanzai.recipe.modules.ingredient.IngredientSaveRequest;
import com.shanzai.recipe.modules.ingredient.IngredientService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/ingredients")
public class AdminIngredientController {
    private final IngredientService ingredientService;

    public AdminIngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public ApiResponse<List<IngredientResponse>> listIngredients(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category
    ) {
        return ApiResponse.ok(ingredientService.listIngredients(keyword, category));
    }

    @PostMapping
    public ApiResponse<IngredientResponse> createIngredient(@Valid @RequestBody IngredientSaveRequest request) {
        return ApiResponse.ok(ingredientService.createIngredient(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<IngredientResponse> updateIngredient(
        @PathVariable Long id,
        @Valid @RequestBody IngredientSaveRequest request
    ) {
        return ApiResponse.ok(ingredientService.updateIngredient(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return ApiResponse.ok(null);
    }
}
