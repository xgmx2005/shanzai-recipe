package com.shanzai.recipe.modules.recommendation.conversation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public record RecommendationConversationContext(
        String intentText,
        String dietGoal,
        List<AvailableIngredientInput> availableIngredients,
        List<String> excludedIngredients,
        List<String> allergyIngredients,
        Integer cookingTime,
        Integer servings,
        List<String> unknownTerms,
        List<String> conflicts,
        boolean restrictionsConfirmed
) {
    public RecommendationConversationContext {
        excludedIngredients = immutableList(excludedIngredients);
        allergyIngredients = immutableList(allergyIngredients);
        availableIngredients = filterRestrictedIngredients(
                immutableList(availableIngredients), excludedIngredients, allergyIngredients);
        unknownTerms = immutableList(unknownTerms);
        conflicts = immutableList(conflicts);
    }

    public RecommendationConversationContext(
            String intentText,
            String dietGoal,
            List<AvailableIngredientInput> availableIngredients,
            List<String> excludedIngredients,
            List<String> allergyIngredients,
            Integer cookingTime,
            Integer servings,
            List<String> unknownTerms,
            List<String> conflicts
    ) {
        this(intentText, dietGoal, availableIngredients, excludedIngredients, allergyIngredients,
                cookingTime, servings, unknownTerms, conflicts, false);
    }

    public static RecommendationConversationContext empty() {
        return new RecommendationConversationContext(
                null, null, List.of(), List.of(), List.of(), null, null,
                List.of(), List.of(), false
        );
    }

    public RecommendationConversationContext merge(ConversationAnswerAnalysis analysis) {
        if (analysis == null || !analysis.relevant()) {
            return this;
        }

        return new RecommendationConversationContext(
                preferNewText(intentText, analysis.intentText()),
                preferNewText(dietGoal, analysis.dietGoal()),
                mergeIngredients(availableIngredients, analysis.availableIngredients()),
                analysis.clearRestrictions() ? List.of() : mergeList(excludedIngredients, analysis.excludedIngredients()),
                analysis.clearRestrictions() ? List.of() : mergeList(allergyIngredients, analysis.allergyIngredients()),
                analysis.cookingTime() == null ? cookingTime : analysis.cookingTime(),
                analysis.servings() == null ? servings : analysis.servings(),
                analysis.unknownTerms(),
                analysis.conflicts(),
                restrictionsConfirmed || analysis.restrictionsAnswered()
        );
    }

    private static String preferNewText(String previous, String next) {
        return hasText(next) ? next : previous;
    }

    private static boolean hasText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static <T> List<T> mergeList(List<T> previous, List<T> next) {
        LinkedHashSet<T> merged = new LinkedHashSet<>(previous);
        merged.addAll(next);
        return List.copyOf(new ArrayList<>(merged));
    }

    private static List<AvailableIngredientInput> mergeIngredients(
            List<AvailableIngredientInput> previous,
            List<AvailableIngredientInput> next
    ) {
        LinkedHashMap<String, AvailableIngredientInput> merged = new LinkedHashMap<>();
        for (AvailableIngredientInput ingredient : previous) {
            merged.put(ingredientKey(ingredient), ingredient);
        }
        for (AvailableIngredientInput ingredient : next) {
            merged.put(ingredientKey(ingredient), ingredient);
        }
        return List.copyOf(merged.values());
    }

    private static String ingredientKey(AvailableIngredientInput ingredient) {
        return ingredient.name() == null
                ? ""
                : ingredient.name().trim().toLowerCase(Locale.ROOT);
    }

    private static List<AvailableIngredientInput> filterRestrictedIngredients(
            List<AvailableIngredientInput> ingredients,
            List<String> excluded,
            List<String> allergies
    ) {
        LinkedHashSet<String> restricted = new LinkedHashSet<>();
        restricted.addAll(normalizedNames(excluded));
        restricted.addAll(normalizedNames(allergies));
        return ingredients.stream()
                .filter(ingredient -> !restricted.contains(ingredientKey(ingredient)))
                .toList();
    }

    private static List<String> normalizedNames(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
