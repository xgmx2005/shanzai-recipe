package com.shanzai.recipe.modules.recommendation.conversation;

public final class ConversationFlow {
    public ConversationTransition apply(
            ConversationStage stage,
            ConversationStatus status,
            RecommendationConversationContext context,
            int invalidAnswerCount,
            ConversationAnswerAnalysis analysis
    ) {
        RecommendationConversationContext currentContext = context == null
                ? RecommendationConversationContext.empty()
                : context;

        if (analysis == null || !analysis.relevant()) {
            int nextInvalidAnswerCount = Math.max(0, invalidAnswerCount) + 1;
            return new ConversationTransition(
                    stage,
                    ConversationStatus.ACTIVE,
                    currentContext,
                    nextInvalidAnswerCount,
                    guidanceFor(nextInvalidAnswerCount)
            );
        }

        RecommendationConversationContext mergedContext = currentContext.merge(analysis);
        if (stage == ConversationStage.RESTRICTIONS) {
            mergedContext = mergedContext.confirmRestrictions();
        }

        ConversationStage nextStage = firstMissingStage(mergedContext);
        boolean needsClarification = !mergedContext.unknownTerms().isEmpty()
                || !mergedContext.conflicts().isEmpty();
        if (nextStage == ConversationStage.CONFIRM && !needsClarification) {
            return new ConversationTransition(
                    ConversationStage.CONFIRM,
                    ConversationStatus.READY_TO_CONFIRM,
                    mergedContext,
                    0,
                    GuidanceMode.NORMAL
            );
        }

        return new ConversationTransition(
                nextStage,
                ConversationStatus.ACTIVE,
                mergedContext,
                0,
                GuidanceMode.NORMAL
        );
    }

    public ConversationStage firstMissingStage(RecommendationConversationContext context) {
        if (context == null || !hasText(context.intentText(), context.dietGoal())) {
            return ConversationStage.INTENT;
        }
        if (context.availableIngredients().isEmpty()) {
            return ConversationStage.INGREDIENTS;
        }
        if (!context.restrictionsConfirmed()) {
            return ConversationStage.RESTRICTIONS;
        }
        if (context.cookingTime() == null || context.cookingTime() <= 0
                || context.servings() == null || context.servings() < 1) {
            return ConversationStage.CONTEXT;
        }
        return ConversationStage.CONFIRM;
    }

    private static GuidanceMode guidanceFor(int invalidAnswerCount) {
        return switch (invalidAnswerCount) {
            case 1 -> GuidanceMode.EXAMPLE;
            case 2 -> GuidanceMode.QUICK_OPTIONS;
            default -> GuidanceMode.RESTART_OPTION;
        };
    }

    private static boolean hasText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }
}
