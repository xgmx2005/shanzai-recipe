package com.shanzai.recipe.modules.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.modules.auth.UserMapper;
import com.shanzai.recipe.modules.ingredient.IngredientMapper;
import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import com.shanzai.recipe.modules.recommendation.RecommendationHistoryEntity;
import com.shanzai.recipe.modules.recommendation.RecommendationHistoryMapper;
import com.shanzai.recipe.modules.recommendation.RecommendationLogEntity;
import com.shanzai.recipe.modules.recommendation.RecommendationLogMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {
    private final UserMapper userMapper;
    private final RecipeMapper recipeMapper;
    private final IngredientMapper ingredientMapper;
    private final RecommendationHistoryMapper historyMapper;
    private final RecommendationLogMapper logMapper;

    public AdminDashboardService(
        UserMapper userMapper,
        RecipeMapper recipeMapper,
        IngredientMapper ingredientMapper,
        RecommendationHistoryMapper historyMapper,
        RecommendationLogMapper logMapper
    ) {
        this.userMapper = userMapper;
        this.recipeMapper = recipeMapper;
        this.ingredientMapper = ingredientMapper;
        this.historyMapper = historyMapper;
        this.logMapper = logMapper;
    }

    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
            userMapper.selectCount(null),
            recipeMapper.selectCount(null),
            ingredientMapper.selectCount(null),
            historyMapper.selectCount(null)
        );
    }

    public List<PopularRecipeStatResponse> listPopularRecipes(Integer limit) {
        List<RecommendationLogEntity> logs = logMapper.selectList(new LambdaQueryWrapper<RecommendationLogEntity>());
        List<Map.Entry<Long, Long>> entries = logs.stream()
            .map(RecommendationLogEntity::getRecipeId)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .limit(limit == null || limit <= 0 ? 10 : limit)
            .toList();
        if (entries.isEmpty()) {
            return List.of();
        }
        List<Long> recipeIds = entries.stream().map(Map.Entry::getKey).toList();
        Map<Long, RecipeEntity> recipesById = recipeMapper.selectBatchIds(recipeIds).stream()
            .collect(Collectors.toMap(
                RecipeEntity::getId,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
            ));
        return entries.stream()
            .map(entry -> new PopularRecipeStatResponse(
                entry.getKey(),
                recipeName(recipesById.get(entry.getKey())),
                entry.getValue()
            ))
            .toList();
    }

    public List<DietGoalStatResponse> listDietGoalStats() {
        return historyMapper.selectList(new LambdaQueryWrapper<RecommendationHistoryEntity>())
            .stream()
            .map(RecommendationHistoryEntity::getDietGoal)
            .filter(goal -> goal != null && !goal.isBlank())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .map(entry -> new DietGoalStatResponse(entry.getKey(), entry.getValue()))
            .toList();
    }

    private String recipeName(RecipeEntity recipe) {
        return recipe == null ? "未知菜谱" : recipe.getName();
    }
}
