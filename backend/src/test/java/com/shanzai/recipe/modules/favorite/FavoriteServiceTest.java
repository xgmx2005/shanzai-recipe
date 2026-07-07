package com.shanzai.recipe.modules.favorite;

import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FavoriteServiceTest {
    private FavoriteMapper favoriteMapper;
    private RecipeMapper recipeMapper;
    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteMapper = mock(FavoriteMapper.class);
        recipeMapper = mock(RecipeMapper.class);
        favoriteService = new FavoriteService(favoriteMapper, recipeMapper);
    }

    @Test
    void repeatedFavoriteCreationReturnsExistingRecordWithoutDuplicateInsert() {
        when(recipeMapper.selectById(1L)).thenReturn(recipe(1L, "番茄鸡蛋"));
        when(favoriteMapper.selectOne(any())).thenReturn(favorite(8L, 7L, 1L));

        FavoriteRecipeResponse response = favoriteService.favoriteRecipe(7L, 1L);

        assertEquals(8L, response.favoriteId());
        assertEquals(1L, response.recipeId());
        assertEquals("番茄鸡蛋", response.recipeName());
        verify(favoriteMapper, never()).insert(any(FavoriteEntity.class));
    }

    private FavoriteEntity favorite(Long id, Long userId, Long recipeId) {
        FavoriteEntity favorite = new FavoriteEntity();
        favorite.setId(id);
        favorite.setUserId(userId);
        favorite.setRecipeId(recipeId);
        favorite.setCreatedAt(LocalDateTime.of(2026, 7, 7, 9, 0));
        return favorite;
    }

    private RecipeEntity recipe(Long id, String name) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(id);
        recipe.setName(name);
        recipe.setDescription(name + "描述");
        recipe.setImageUrl("/images/recipes/test.jpg");
        recipe.setCalories(320);
        recipe.setProtein(new BigDecimal("18.00"));
        recipe.setStatus(1);
        return recipe;
    }
}
