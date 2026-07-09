package com.shanzai.recipe.modules.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.common.DietGoal;
import com.shanzai.recipe.modules.ingredient.IngredientEntity;
import com.shanzai.recipe.modules.ingredient.IngredientMapper;
import com.shanzai.recipe.modules.profile.ProfileEntity;
import com.shanzai.recipe.modules.profile.ProfileMapper;
import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeIngredientEntity;
import com.shanzai.recipe.modules.recipe.RecipeIngredientMapper;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RecommendationService {
    private static final int ACTIVE_STATUS = 1;
    private static final int MAX_RECOMMENDATIONS = 5;

    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final IngredientMapper ingredientMapper;
    private final ProfileMapper profileMapper;
    private final RecommendationHistoryMapper historyMapper;
    private final RecommendationLogMapper logMapper;
    private final RecommendationScoringService scoringService;
    private final AiRecommendationService aiRecommendationService;

    public RecommendationService(
        RecipeMapper recipeMapper,
        RecipeIngredientMapper recipeIngredientMapper,
        IngredientMapper ingredientMapper,
        ProfileMapper profileMapper,
        RecommendationHistoryMapper historyMapper,
        RecommendationLogMapper logMapper,
        RecommendationScoringService scoringService,
        AiRecommendationService aiRecommendationService
    ) {
        this.recipeMapper = recipeMapper;
        this.recipeIngredientMapper = recipeIngredientMapper;
        this.ingredientMapper = ingredientMapper;
        this.profileMapper = profileMapper;
        this.historyMapper = historyMapper;
        this.logMapper = logMapper;
        this.scoringService = scoringService;
        this.aiRecommendationService = aiRecommendationService;
    }

    @Transactional
    public RecommendationResponse recommend(Long userId, RecommendationRequest request) {
        ProfileEntity profile = findProfile(userId);
        RecommendationRequestModel requestModel = toRequestModel(request, profile);

        List<RecipeEntity> recipes = recipeMapper.selectList(new LambdaQueryWrapper<RecipeEntity>().orderByAsc(RecipeEntity::getId))
            .stream()
            .filter(recipe -> Objects.equals(recipe.getStatus(), ACTIVE_STATUS))
            .toList();
        List<RecipeIngredientEntity> rows = recipeIngredientMapper.selectList(new LambdaQueryWrapper<RecipeIngredientEntity>());
        Map<Long, IngredientEntity> ingredientsById = ingredientsById(rows);
        Map<Long, List<RecipeIngredientEntity>> rowsByRecipeId = rows.stream()
            .collect(Collectors.groupingBy(
                RecipeIngredientEntity::getRecipeId,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<ScoredRecipe> scoredRecipes = recipes.stream()
            .map(recipe -> scoreRecipe(recipe, rowsByRecipeId.getOrDefault(recipe.getId(), List.of()), ingredientsById, requestModel))
            .filter(scoredRecipe -> scoredRecipe.score().eligible())
            .sorted(Comparator.comparingInt((ScoredRecipe scoredRecipe) -> scoredRecipe.score().totalScore()).reversed())
            .limit(MAX_RECOMMENDATIONS)
            .toList();

        AiRecommendationAnalysis analysis = aiRecommendationService.generateAnalysis(toAiContext(requestModel, scoredRecipes));
        List<RecommendedRecipeResponse> recommendedRecipes = IntStream.range(0, scoredRecipes.size())
            .mapToObj(index -> toResponse(
                scoredRecipes.get(index),
                requestModel,
                index == 0 ? analysis.topRecipeReason() : null
            ))
            .toList();
        RecommendationHistoryEntity history = saveHistory(userId, request, requestModel, recommendedRecipes, analysis);
        saveLogs(userId, requestModel, scoredRecipes);

        return new RecommendationResponse(
            history.getId(),
            analysis.summary(),
            analysis.healthTip(),
            analysis.shoppingTip(),
            analysis.generated(),
            recommendedRecipes
        );
    }

    private ScoredRecipe scoreRecipe(
        RecipeEntity recipe,
        List<RecipeIngredientEntity> rows,
        Map<Long, IngredientEntity> ingredientsById,
        RecommendationRequestModel requestModel
    ) {
        RecipeCandidate candidate = toCandidate(recipe, rows, ingredientsById);
        RecommendationScore score = scoringService.score(candidate, requestModel);
        return new ScoredRecipe(recipe, candidate, score);
    }

    private RecipeCandidate toCandidate(
        RecipeEntity recipe,
        List<RecipeIngredientEntity> rows,
        Map<Long, IngredientEntity> ingredientsById
    ) {
        List<String> ingredients = rows.stream()
            .map(row -> ingredientsById.get(row.getIngredientId()))
            .filter(Objects::nonNull)
            .map(IngredientEntity::getName)
            .toList();
        List<String> coreIngredients = rows.stream()
            .filter(row -> Boolean.TRUE.equals(row.getCore()))
            .map(row -> ingredientsById.get(row.getIngredientId()))
            .filter(Objects::nonNull)
            .map(IngredientEntity::getName)
            .toList();

        return new RecipeCandidate(
            recipe.getId(),
            recipe.getName(),
            ingredients,
            coreIngredients,
            splitList(recipe.getTargetGoals()),
            mergeTags(recipe.getTasteTags(), recipe.getHealthTags()),
            recipe.getCookingTime(),
            0
        );
    }

    private RecommendedRecipeResponse toResponse(
        ScoredRecipe scoredRecipe,
        RecommendationRequestModel requestModel,
        String topAiReason
    ) {
        List<String> matchedIngredients = matchedIngredients(
            scoredRecipe.candidate().ingredients(),
            requestModel.availableIngredients()
        );
        String reason = topAiReason == null || topAiReason.isBlank()
            ? aiRecommendationService.generateLocalReason(
                scoredRecipe.recipe().getName(),
                requestModel.dietGoal(),
                matchedIngredients
            )
            : topAiReason;
        return new RecommendedRecipeResponse(
            scoredRecipe.recipe().getId(),
            scoredRecipe.recipe().getName(),
            scoredRecipe.score().totalScore(),
            reason,
            scoredRecipe.recipe().getCalories(),
            scoredRecipe.recipe().getProtein(),
            scoredRecipe.recipe().getImageUrl()
        );
    }

    private AiRecommendationContext toAiContext(
        RecommendationRequestModel requestModel,
        List<ScoredRecipe> scoredRecipes
    ) {
        return new AiRecommendationContext(
            requestModel.dietGoal(),
            requestModel.availableIngredients(),
            requestModel.excludedIngredients(),
            requestModel.cookingTime(),
            scoredRecipes.stream()
                .map(scoredRecipe -> new AiRecommendationContext.RecipeSnapshot(
                    scoredRecipe.recipe().getName(),
                    scoredRecipe.score().totalScore(),
                    scoredRecipe.recipe().getCalories(),
                    decimalText(scoredRecipe.recipe().getProtein()),
                    matchedIngredients(
                        scoredRecipe.candidate().ingredients(),
                        requestModel.availableIngredients()
                    ),
                    scoredRecipe.candidate().tags()
                ))
                .toList()
        );
    }

    private RecommendationHistoryEntity saveHistory(
        Long userId,
        RecommendationRequest request,
        RecommendationRequestModel requestModel,
        List<RecommendedRecipeResponse> recipes,
        AiRecommendationAnalysis analysis
    ) {
        RecommendationHistoryEntity history = new RecommendationHistoryEntity();
        history.setUserId(userId);
        history.setInputIngredients(joinList(request.availableIngredients()));
        history.setExcludedIngredients(joinList(requestModel.excludedIngredients()));
        history.setDietGoal(requestModel.dietGoal());
        history.setCookingTime(requestModel.cookingTime());
        history.setServings(defaultServings(request.servings()));
        history.setResultRecipeIds(recipes.stream()
            .map(recipe -> String.valueOf(recipe.id()))
            .collect(Collectors.joining(",")));
        history.setAiSummary(analysis.summary());
        history.setAiHealthTip(analysis.healthTip());
        history.setAiShoppingTip(analysis.shoppingTip());
        history.setAiGenerated(analysis.generated());
        historyMapper.insert(history);
        return history;
    }

    private void saveLogs(Long userId, RecommendationRequestModel requestModel, List<ScoredRecipe> scoredRecipes) {
        String snapshot = "available=" + joinList(requestModel.availableIngredients())
            + "; excluded=" + joinList(requestModel.excludedIngredients())
            + "; blocked=" + joinList(requestModel.blockedIngredients());
        scoredRecipes.forEach(scoredRecipe -> {
            RecommendationLogEntity log = new RecommendationLogEntity();
            log.setUserId(userId);
            log.setRecipeId(scoredRecipe.recipe().getId());
            log.setDietGoal(requestModel.dietGoal());
            log.setScore(BigDecimal.valueOf(scoredRecipe.score().totalScore()));
            log.setInputSnapshot(snapshot);
            logMapper.insert(log);
        });
    }

    private RecommendationRequestModel toRequestModel(RecommendationRequest request, ProfileEntity profile) {
        String dietGoal = request.dietGoal() == null
            ? defaultDietGoal(profile)
            : request.dietGoal().name();
        Integer cookingTime = request.cookingTime() == null
            ? defaultCookingTime(profile)
            : request.cookingTime();
        List<String> blockedIngredients = mergeLists(
            splitList(profile == null ? null : profile.getAvoidIngredients()),
            splitList(profile == null ? null : profile.getAllergyIngredients())
        );
        return new RecommendationRequestModel(
            cleanList(request.availableIngredients()),
            cleanList(request.excludedIngredients()),
            blockedIngredients,
            dietGoal,
            splitList(profile == null ? null : profile.getTastePreferences()),
            cookingTime
        );
    }

    private ProfileEntity findProfile(Long userId) {
        return profileMapper.selectOne(new LambdaQueryWrapper<ProfileEntity>().eq(ProfileEntity::getUserId, userId));
    }

    private Map<Long, IngredientEntity> ingredientsById(List<RecipeIngredientEntity> rows) {
        List<Long> ids = rows.stream()
            .map(RecipeIngredientEntity::getIngredientId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<IngredientEntity> ingredients = ingredientMapper.selectBatchIds(ids);
        if (ingredients == null) {
            return Map.of();
        }
        return ingredients.stream()
            .collect(Collectors.toMap(
                IngredientEntity::getId,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
            ));
    }

    private List<String> matchedIngredients(List<String> ingredients, List<String> availableIngredients) {
        LinkedHashSet<String> available = new LinkedHashSet<>(cleanList(availableIngredients));
        return cleanList(ingredients).stream()
            .filter(available::contains)
            .toList();
    }

    private List<String> mergeTags(String tasteTags, String healthTags) {
        return mergeLists(splitList(tasteTags), splitList(healthTags));
    }

    private List<String> mergeLists(List<String> first, List<String> second) {
        List<String> values = new ArrayList<>();
        values.addAll(first);
        values.addAll(second);
        return cleanList(values);
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return cleanList(Arrays.asList(value.split(",")));
    }

    private List<String> cleanList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    private String joinList(List<String> values) {
        return String.join(",", cleanList(values));
    }

    private String decimalText(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String defaultDietGoal(ProfileEntity profile) {
        if (profile == null || profile.getDietGoal() == null || profile.getDietGoal().isBlank()) {
            return DietGoal.BALANCED.name();
        }
        return profile.getDietGoal();
    }

    private Integer defaultCookingTime(ProfileEntity profile) {
        if (profile == null || profile.getCookingTimePreference() == null) {
            return 30;
        }
        return profile.getCookingTimePreference();
    }

    private Integer defaultServings(Integer servings) {
        return servings == null ? 1 : servings;
    }

    private record ScoredRecipe(
        RecipeEntity recipe,
        RecipeCandidate candidate,
        RecommendationScore score
    ) {
    }
}
