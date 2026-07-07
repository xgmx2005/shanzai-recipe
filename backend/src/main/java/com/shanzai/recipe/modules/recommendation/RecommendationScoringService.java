package com.shanzai.recipe.modules.recommendation;

import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationScoringService {
    private static final int INGREDIENT_WEIGHT = 35;
    private static final int GOAL_WEIGHT = 25;
    private static final int TASTE_WEIGHT = 15;
    private static final int CONVENIENCE_WEIGHT = 15;
    private static final int POPULARITY_WEIGHT = 10;

    public RecommendationScore score(RecipeCandidate candidate, RecommendationRequestModel request) {
        if (hasBlockedIngredient(candidate.ingredients(), request.excludedIngredients())
            || hasBlockedIngredient(candidate.ingredients(), request.blockedIngredients())) {
            return RecommendationScore.ineligible("包含忌口或过敏食材");
        }

        Set<String> available = normalize(request.availableIngredients());
        Set<String> candidateIngredients = normalize(candidate.ingredients());
        Set<String> goals = normalize(candidate.targetGoals());
        Set<String> tags = normalize(candidate.tags());
        Set<String> tastes = normalize(request.tastePreferences());

        int ingredientScore = ingredientScore(candidateIngredients, available);
        int goalScore = goals.contains(clean(request.dietGoal())) ? GOAL_WEIGHT : 0;
        int tasteScore = intersects(tags, tastes) ? TASTE_WEIGHT : 0;
        int convenienceScore = convenienceScore(candidate.cookingTime(), request.cookingTime());
        int popularityScore = Math.min(POPULARITY_WEIGHT, Math.max(0, defaultInteger(candidate.popularity())));

        List<String> reasons = buildReasons(
            ingredientScore,
            goalScore,
            tasteScore,
            convenienceScore,
            popularityScore
        );
        int totalScore = ingredientScore + goalScore + tasteScore + convenienceScore + popularityScore;
        return new RecommendationScore(Math.min(100, totalScore), reasons, true);
    }

    private int ingredientScore(Set<String> candidateIngredients, Set<String> available) {
        if (candidateIngredients.isEmpty() || available.isEmpty()) {
            return 0;
        }
        long matched = candidateIngredients.stream().filter(available::contains).count();
        return (int) Math.round(INGREDIENT_WEIGHT * matched / (double) candidateIngredients.size());
    }

    private int convenienceScore(Integer cookingTime, Integer maxCookingTime) {
        if (cookingTime == null || maxCookingTime == null || maxCookingTime <= 0) {
            return 0;
        }
        if (cookingTime <= maxCookingTime) {
            return CONVENIENCE_WEIGHT;
        }
        if (cookingTime <= maxCookingTime + 15) {
            return 8;
        }
        return 0;
    }

    private List<String> buildReasons(
        int ingredientScore,
        int goalScore,
        int tasteScore,
        int convenienceScore,
        int popularityScore
    ) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        if (ingredientScore >= 18) {
            reasons.add("已有食材匹配度高");
        }
        if (goalScore > 0) {
            reasons.add("符合当前饮食目标");
        }
        if (tasteScore > 0) {
            reasons.add("贴合口味偏好");
        }
        if (convenienceScore > 0) {
            reasons.add("烹饪时间合适");
        }
        if (popularityScore > 0) {
            reasons.add("历史推荐表现较好");
        }
        if (reasons.isEmpty()) {
            reasons.add("可作为备选菜谱");
        }
        return List.copyOf(reasons);
    }

    private boolean hasBlockedIngredient(List<String> ingredients, List<String> blocked) {
        Set<String> ingredientSet = normalize(ingredients);
        return normalize(blocked).stream().anyMatch(ingredientSet::contains);
    }

    private boolean intersects(Set<String> first, Set<String> second) {
        return first.stream().anyMatch(second::contains);
    }

    private Set<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(this::clean)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }
}
