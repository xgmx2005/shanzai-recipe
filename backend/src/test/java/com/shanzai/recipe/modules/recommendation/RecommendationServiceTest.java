package com.shanzai.recipe.modules.recommendation;

import com.shanzai.recipe.common.DietGoal;
import com.shanzai.recipe.modules.ingredient.IngredientEntity;
import com.shanzai.recipe.modules.ingredient.IngredientMapper;
import com.shanzai.recipe.modules.profile.ProfileEntity;
import com.shanzai.recipe.modules.profile.ProfileMapper;
import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeIngredientEntity;
import com.shanzai.recipe.modules.recipe.RecipeIngredientMapper;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {
    private RecipeMapper recipeMapper;
    private RecipeIngredientMapper recipeIngredientMapper;
    private IngredientMapper ingredientMapper;
    private ProfileMapper profileMapper;
    private RecommendationHistoryMapper historyMapper;
    private RecommendationLogMapper logMapper;
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recipeMapper = mock(RecipeMapper.class);
        recipeIngredientMapper = mock(RecipeIngredientMapper.class);
        ingredientMapper = mock(IngredientMapper.class);
        profileMapper = mock(ProfileMapper.class);
        historyMapper = mock(RecommendationHistoryMapper.class);
        logMapper = mock(RecommendationLogMapper.class);
        recommendationService = new RecommendationService(
            recipeMapper,
            recipeIngredientMapper,
            ingredientMapper,
            profileMapper,
            historyMapper,
            logMapper,
            new RecommendationScoringService(),
            new AiRecommendationService(new DisabledDeepSeekClient())
        );
    }

    @Test
    void recommendBuildsCandidatesAndPersistsHistoryAndLogs() {
        when(recipeMapper.selectList(any())).thenReturn(List.of(
            recipe(1L, "鸡胸肉西兰花轻食碗", "FAT_LOSS,MUSCLE_GAIN", "清淡,轻食", "低脂,高蛋白", 1),
            recipe(2L, "花生鸡丁", "BALANCED", "家常", "高蛋白", 1)
        ));
        when(recipeIngredientMapper.selectList(any())).thenReturn(List.of(
            recipeIngredient(1L, 1L, true),
            recipeIngredient(1L, 9L, true),
            recipeIngredient(2L, 1L, true),
            recipeIngredient(2L, 36L, true)
        ));
        when(ingredientMapper.selectBatchIds(List.of(1L, 9L, 36L))).thenReturn(List.of(
            ingredient(1L, "鸡胸肉"),
            ingredient(9L, "西兰花"),
            ingredient(36L, "花生")
        ));
        when(profileMapper.selectOne(any())).thenReturn(profile());
        when(historyMapper.insert(any(RecommendationHistoryEntity.class))).thenAnswer(invocation -> {
            RecommendationHistoryEntity history = invocation.getArgument(0);
            history.setId(5L);
            return 1;
        });

        RecommendationResponse response = recommendationService.recommend(
            7L,
            new RecommendationRequest(
                List.of("鸡胸肉", "西兰花"),
                List.of(),
                DietGoal.FAT_LOSS,
                30,
                1
            )
        );

        assertEquals(5L, response.historyId());
        assertEquals(1, response.recipes().size());
        assertEquals(1L, response.recipes().get(0).id());
        assertTrue(response.recipes().get(0).score() >= 80);
        assertTrue(response.aiSummary().contains("推荐"));
        assertTrue(response.aiHealthTip().contains("建议") || response.aiHealthTip().contains("搭配"));
        assertTrue(response.aiShoppingTip().contains("购物") || response.aiShoppingTip().contains("清单"));
        assertEquals(false, response.aiGenerated());

        ArgumentCaptor<RecommendationHistoryEntity> historyCaptor =
            ArgumentCaptor.forClass(RecommendationHistoryEntity.class);
        ArgumentCaptor<RecommendationLogEntity> logCaptor = ArgumentCaptor.forClass(RecommendationLogEntity.class);
        verify(historyMapper).insert(historyCaptor.capture());
        verify(logMapper).insert(logCaptor.capture());
        assertEquals(7L, historyCaptor.getValue().getUserId());
        assertEquals("1", historyCaptor.getValue().getResultRecipeIds());
        assertTrue(historyCaptor.getValue().getAiHealthTip().contains("建议")
            || historyCaptor.getValue().getAiHealthTip().contains("搭配"));
        assertTrue(historyCaptor.getValue().getAiShoppingTip().contains("购物")
            || historyCaptor.getValue().getAiShoppingTip().contains("清单"));
        assertEquals(false, historyCaptor.getValue().getAiGenerated());
        assertEquals(1L, logCaptor.getValue().getRecipeId());
    }

    @Test
    void recommendCallsAiOnlyOnceForRecommendationAnalysis() {
        AtomicInteger aiCallCount = new AtomicInteger();
        recommendationService = new RecommendationService(
            recipeMapper,
            recipeIngredientMapper,
            ingredientMapper,
            profileMapper,
            historyMapper,
            logMapper,
            new RecommendationScoringService(),
            new AiRecommendationService(context -> {
                aiCallCount.incrementAndGet();
                return Optional.of(new AiRecommendationText(
                    "AI总结",
                    "AI健康提示",
                    "AI购物提示",
                    "AI推荐：" + context.recipes().get(0).name()
                ));
            })
        );
        when(recipeMapper.selectList(any())).thenReturn(List.of(
            recipe(1L, "鸡胸肉西兰花轻食碗", "FAT_LOSS", "清淡", "低脂,高蛋白", 1),
            recipe(3L, "西兰花鸡蛋碗", "FAT_LOSS", "清淡", "低脂,高蛋白", 1)
        ));
        when(recipeIngredientMapper.selectList(any())).thenReturn(List.of(
            recipeIngredient(1L, 1L, true),
            recipeIngredient(1L, 9L, true),
            recipeIngredient(3L, 9L, true),
            recipeIngredient(3L, 12L, true)
        ));
        when(ingredientMapper.selectBatchIds(List.of(1L, 9L, 12L))).thenReturn(List.of(
            ingredient(1L, "鸡胸肉"),
            ingredient(9L, "西兰花"),
            ingredient(12L, "鸡蛋")
        ));
        when(profileMapper.selectOne(any())).thenReturn(profile());
        when(historyMapper.insert(any(RecommendationHistoryEntity.class))).thenAnswer(invocation -> {
            RecommendationHistoryEntity history = invocation.getArgument(0);
            history.setId(6L);
            return 1;
        });

        RecommendationResponse response = recommendationService.recommend(
            7L,
            new RecommendationRequest(
                List.of("鸡胸肉", "西兰花", "鸡蛋"),
                List.of(),
                DietGoal.FAT_LOSS,
                30,
                1
            )
        );

        assertEquals(2, response.recipes().size());
        assertEquals("AI推荐：鸡胸肉西兰花轻食碗", response.recipes().get(0).reason());
        assertTrue(response.recipes().get(1).reason().contains("西兰花鸡蛋碗"));
        assertEquals("AI总结", response.aiSummary());
        assertEquals("AI健康提示", response.aiHealthTip());
        assertEquals("AI购物提示", response.aiShoppingTip());
        assertEquals(true, response.aiGenerated());
        assertEquals(1, aiCallCount.get());
    }

    private RecipeEntity recipe(
        Long id,
        String name,
        String targetGoals,
        String tasteTags,
        String healthTags,
        Integer status
    ) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(id);
        recipe.setName(name);
        recipe.setDescription(name + "描述");
        recipe.setImageUrl("/images/recipes/test.jpg");
        recipe.setCookingTime(20);
        recipe.setDifficulty("EASY");
        recipe.setServings(1);
        recipe.setCalories(420);
        recipe.setProtein(new BigDecimal("35.00"));
        recipe.setFat(new BigDecimal("9.00"));
        recipe.setCarbs(new BigDecimal("45.00"));
        recipe.setTargetGoals(targetGoals);
        recipe.setTasteTags(tasteTags);
        recipe.setHealthTags(healthTags);
        recipe.setStatus(status);
        return recipe;
    }

    private RecipeIngredientEntity recipeIngredient(Long recipeId, Long ingredientId, boolean core) {
        RecipeIngredientEntity row = new RecipeIngredientEntity();
        row.setRecipeId(recipeId);
        row.setIngredientId(ingredientId);
        row.setQuantity(new BigDecimal("100"));
        row.setUnit("g");
        row.setCore(core);
        return row;
    }

    private IngredientEntity ingredient(Long id, String name) {
        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setId(id);
        ingredient.setName(name);
        ingredient.setCategory("食材");
        ingredient.setUnit("g");
        return ingredient;
    }

    private ProfileEntity profile() {
        ProfileEntity profile = new ProfileEntity();
        profile.setUserId(7L);
        profile.setDietGoal("FAT_LOSS");
        profile.setTastePreferences("清淡");
        profile.setAvoidIngredients("花生");
        profile.setAllergyIngredients("");
        profile.setCookingTimePreference(30);
        return profile;
    }
}
