package com.shanzai.recipe.modules.recommendation.conversation;

public final class ConversationFlow {
    public ConversationTransition apply(
            ConversationStage stage,
            ConversationStatus status,
            RecommendationConversationContext context,
            int invalidAnswerCount,
            ConversationAnswerAnalysis analysis
    ) {
        validateLifecycle(stage, status);

        RecommendationConversationContext currentContext = context == null
                ? RecommendationConversationContext.empty()
                : context;

        if (analysis == null || !analysis.relevant()) {
            int nextInvalidAnswerCount = incrementInvalidAnswerCount(invalidAnswerCount);
            return new ConversationTransition(
                    stage,
                    status,
                    currentContext,
                    nextInvalidAnswerCount,
                    guidanceFor(nextInvalidAnswerCount)
            );
        }

        RecommendationConversationContext mergedContext = currentContext.merge(analysis);
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

    private static void validateLifecycle(ConversationStage stage, ConversationStatus status) {
        if (stage == null) {
            throw new IllegalArgumentException("conversation stage must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("conversation status must not be null");
        }
        if (status == ConversationStatus.COMPLETED || status == ConversationStatus.CANCELLED) {
            throw new IllegalStateException("terminal conversation cannot migrate");
        }
    }

    private static int incrementInvalidAnswerCount(int invalidAnswerCount) {
        if (invalidAnswerCount >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, invalidAnswerCount) + 1;
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
