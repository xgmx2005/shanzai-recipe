package com.shanzai.recipe.modules.favorite;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FavoriteService {
    private static final int ACTIVE_RECIPE_STATUS = 1;

    private final FavoriteMapper favoriteMapper;
    private final RecipeMapper recipeMapper;

    public FavoriteService(FavoriteMapper favoriteMapper, RecipeMapper recipeMapper) {
        this.favoriteMapper = favoriteMapper;
        this.recipeMapper = recipeMapper;
    }

    @Transactional
    public FavoriteRecipeResponse favoriteRecipe(Long userId, Long recipeId) {
        RecipeEntity recipe = findActiveRecipe(recipeId);
        FavoriteEntity existing = findExisting(userId, recipeId);
        if (existing != null) {
            return toResponse(existing, recipe);
        }

        FavoriteEntity favorite = new FavoriteEntity();
        favorite.setUserId(userId);
        favorite.setRecipeId(recipeId);
        favoriteMapper.insert(favorite);
        return toResponse(favorite, recipe);
    }

    @Transactional
    public void unfavoriteRecipe(Long userId, Long recipeId) {
        favoriteMapper.delete(
            new LambdaQueryWrapper<FavoriteEntity>()
                .eq(FavoriteEntity::getUserId, userId)
                .eq(FavoriteEntity::getRecipeId, recipeId)
        );
    }

    public List<FavoriteRecipeResponse> listFavorites(Long userId) {
        List<FavoriteEntity> favorites = favoriteMapper.selectList(
            new LambdaQueryWrapper<FavoriteEntity>()
                .eq(FavoriteEntity::getUserId, userId)
                .orderByDesc(FavoriteEntity::getId)
        );
        List<Long> recipeIds = favorites.stream()
            .map(FavoriteEntity::getRecipeId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (recipeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, RecipeEntity> recipesById = recipeMapper.selectBatchIds(recipeIds).stream()
            .filter(recipe -> Objects.equals(recipe.getStatus(), ACTIVE_RECIPE_STATUS))
            .collect(Collectors.toMap(
                RecipeEntity::getId,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
            ));
        return favorites.stream()
            .filter(favorite -> recipesById.containsKey(favorite.getRecipeId()))
            .map(favorite -> toResponse(favorite, recipesById.get(favorite.getRecipeId())))
            .toList();
    }

    private FavoriteEntity findExisting(Long userId, Long recipeId) {
        return favoriteMapper.selectOne(
            new LambdaQueryWrapper<FavoriteEntity>()
                .eq(FavoriteEntity::getUserId, userId)
                .eq(FavoriteEntity::getRecipeId, recipeId)
        );
    }

    private RecipeEntity findActiveRecipe(Long recipeId) {
        RecipeEntity recipe = recipeMapper.selectById(recipeId);
        if (recipe == null || !Objects.equals(recipe.getStatus(), ACTIVE_RECIPE_STATUS)) {
            throw new BusinessException("菜谱不存在或已下架");
        }
        return recipe;
    }

    private FavoriteRecipeResponse toResponse(FavoriteEntity favorite, RecipeEntity recipe) {
        return new FavoriteRecipeResponse(
            favorite.getId(),
            recipe.getId(),
            recipe.getName(),
            recipe.getDescription(),
            recipe.getImageUrl(),
            recipe.getCalories(),
            recipe.getProtein(),
            favorite.getCreatedAt()
        );
    }
}
