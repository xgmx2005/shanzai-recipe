package com.shanzai.recipe.modules.recommendation.conversation;

import java.math.BigDecimal;
import java.util.List;

public record ConversationAnswerAnalysis(
        boolean relevant,
        String intentText,
        String dietGoal,
        List<AvailableIngredientInput> availableIngredients,
        List<String> excludedIngredients,
        List<String> allergyIngredients,
        Integer cookingTime,
        Integer servings,
        List<String> unknownTerms,
        List<String> conflicts,
        BigDecimal confidence,
        boolean restrictionsAnswered
) {
    public ConversationAnswerAnalysis {
        availableIngredients = immutableList(availableIngredients);
        excludedIngredients = immutableList(excludedIngredients);
        allergyIngredients = immutableList(allergyIngredients);
        unknownTerms = immutableList(unknownTerms);
        conflicts = immutableList(conflicts);
    }

    public ConversationAnswerAnalysis(
            boolean relevant,
            String intentText,
            String dietGoal,
            List<AvailableIngredientInput> availableIngredients,
            List<String> excludedIngredients,
            List<String> allergyIngredients,
            Integer cookingTime,
            Integer servings,
            List<String> unknownTerms,
            List<String> conflicts,
            BigDecimal confidence
    ) {
        this(relevant, intentText, dietGoal, availableIngredients, excludedIngredients, allergyIngredients,
                cookingTime, servings, unknownTerms, conflicts, confidence, false);
    }

    public ConversationAnswerAnalysis(
            boolean relevant,
            String intentText,
            String dietGoal,
            List<AvailableIngredientInput> availableIngredients,
            List<String> excludedIngredients,
            List<String> allergyIngredients,
            Integer cookingTime,
            Integer servings,
            List<String> unknownTerms,
            List<String> conflicts,
            boolean restrictionsAnswered,
            BigDecimal confidence
    ) {
        this(relevant, intentText, dietGoal, availableIngredients, excludedIngredients, allergyIngredients,
                cookingTime, servings, unknownTerms, conflicts, confidence, restrictionsAnswered);
    }

    public static ConversationAnswerAnalysis invalid() {
        return new ConversationAnswerAnalysis(
                false, null, null, List.of(), List.of(), List.of(),
                null, null, List.of(), List.of(), BigDecimal.ZERO, false
        );
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
