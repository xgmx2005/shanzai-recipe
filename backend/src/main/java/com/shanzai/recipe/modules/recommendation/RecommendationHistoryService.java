package com.shanzai.recipe.modules.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationHistoryService {
    private final RecommendationHistoryMapper historyMapper;
    private final RecipeMapper recipeMapper;

    public RecommendationHistoryService(RecommendationHistoryMapper historyMapper, RecipeMapper recipeMapper) {
        this.historyMapper = historyMapper;
        this.recipeMapper = recipeMapper;
    }

    public List<RecommendationHistorySummaryResponse> listHistories(Long userId) {
        return historyMapper.selectList(
            new LambdaQueryWrapper<RecommendationHistoryEntity>()
                .eq(RecommendationHistoryEntity::getUserId, userId)
                .orderByDesc(RecommendationHistoryEntity::getId)
        ).stream()
            .map(this::toSummary)
            .toList();
    }

    public RecommendationHistoryDetailResponse getHistory(Long userId, Long id) {
        RecommendationHistoryEntity history = historyMapper.selectById(id);
        if (history == null || !Objects.equals(history.getUserId(), userId)) {
            throw new BusinessException("推荐历史不存在");
        }
        List<Long> recipeIds = splitIds(history.getResultRecipeIds());
        Map<Long, RecipeEntity> recipesById = recipesById(recipeIds);
        List<RecommendationHistoryRecipeResponse> recipes = recipeIds.stream()
            .map(recipesById::get)
            .filter(Objects::nonNull)
            .map(this::toRecipeResponse)
            .toList();
        return new RecommendationHistoryDetailResponse(
            history.getId(),
            splitList(history.getInputIngredients()),
            splitList(history.getExcludedIngredients()),
            history.getDietGoal(),
            history.getCookingTime(),
            history.getServings(),
            recipeIds,
            history.getAiSummary(),
            recipes,
            history.getCreatedAt()
        );
    }

    private RecommendationHistorySummaryResponse toSummary(RecommendationHistoryEntity history) {
        return new RecommendationHistorySummaryResponse(
            history.getId(),
            splitList(history.getInputIngredients()),
            splitList(history.getExcludedIngredients()),
            history.getDietGoal(),
            history.getCookingTime(),
            history.getServings(),
            splitIds(history.getResultRecipeIds()),
            history.getAiSummary(),
            history.getCreatedAt()
        );
    }

    private Map<Long, RecipeEntity> recipesById(List<Long> recipeIds) {
        if (recipeIds.isEmpty()) {
            return Map.of();
        }
        List<RecipeEntity> recipes = recipeMapper.selectBatchIds(recipeIds);
        if (recipes == null) {
            return Map.of();
        }
        return recipes.stream()
            .collect(Collectors.toMap(
                RecipeEntity::getId,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
            ));
    }

    private RecommendationHistoryRecipeResponse toRecipeResponse(RecipeEntity recipe) {
        return new RecommendationHistoryRecipeResponse(
            recipe.getId(),
            recipe.getName(),
            recipe.getImageUrl(),
            recipe.getCalories(),
            recipe.getProtein()
        );
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .toList();
    }

    private List<Long> splitIds(String value) {
        return splitList(value).stream().map(Long::valueOf).toList();
    }
}
