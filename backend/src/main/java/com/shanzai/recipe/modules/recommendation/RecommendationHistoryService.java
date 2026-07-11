package com.shanzai.recipe.modules.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import com.shanzai.recipe.modules.recommendation.conversation.RecommendationConversationContext;
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
    private final ObjectMapper objectMapper;

    public RecommendationHistoryService(
        RecommendationHistoryMapper historyMapper,
        RecipeMapper recipeMapper,
        ObjectMapper objectMapper
    ) {
        this.historyMapper = historyMapper;
        this.recipeMapper = recipeMapper;
        this.objectMapper = objectMapper;
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
        RecommendationHistoryEntity history = requireOwnedHistory(userId, id);
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
            readConversationContext(history.getConversationContextJson()),
            history.getDietGoal(),
            history.getCookingTime(),
            history.getServings(),
            recipeIds,
            history.getAiSummary(),
            history.getAiHealthTip(),
            history.getAiShoppingTip(),
            Boolean.TRUE.equals(history.getAiGenerated()),
            recipes,
            history.getCreatedAt()
        );
    }

    public void attachConversationContext(Long userId, Long historyId, RecommendationConversationContext context) {
        RecommendationHistoryEntity history = requireOwnedHistory(userId, historyId);
        history.setConversationContextJson(writeConversationContext(context));
        historyMapper.updateById(history);
    }

    public RecommendationResponse getRecommendationResponse(Long userId, Long historyId) {
        RecommendationHistoryDetailResponse detail = getHistory(userId, historyId);
        List<RecommendedRecipeResponse> recipes = detail.recipes().stream()
            .map(recipe -> new RecommendedRecipeResponse(
                recipe.id(),
                recipe.name(),
                0,
                "已保存的推荐结果",
                recipe.calories(),
                recipe.protein(),
                recipe.imageUrl()
            ))
            .toList();
        return new RecommendationResponse(
            detail.id(),
            detail.aiSummary(),
            detail.aiHealthTip(),
            detail.aiShoppingTip(),
            detail.aiGenerated(),
            recipes
        );
    }

    private RecommendationHistoryEntity requireOwnedHistory(Long userId, Long id) {
        RecommendationHistoryEntity history = historyMapper.selectById(id);
        if (history == null || !Objects.equals(history.getUserId(), userId)) {
            throw new BusinessException("推荐历史不存在");
        }
        return history;
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
            history.getAiHealthTip(),
            history.getAiShoppingTip(),
            Boolean.TRUE.equals(history.getAiGenerated()),
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

    private RecommendationConversationContext readConversationContext(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, RecommendationConversationContext.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to read conversation context", exception);
        }
    }

    private String writeConversationContext(RecommendationConversationContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to write conversation context", exception);
        }
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
