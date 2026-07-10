package com.shanzai.recipe.modules.recommendation.conversation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationFlowTest {
    private final ConversationFlow flow = new ConversationFlow();

    @Test
    void advancesPastFieldsAlreadyAnsweredInOneMessage() {
        ConversationAnswerAnalysis analysis = completeAnalysis(List.of(), List.of());

        ConversationTransition transition = flow.apply(
                ConversationStage.INTENT,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                0,
                analysis
        );

        assertEquals(ConversationStage.CONFIRM, transition.stage());
        assertEquals(ConversationStatus.READY_TO_CONFIRM, transition.status());
        assertEquals(0, transition.invalidAnswerCount());
        assertEquals(GuidanceMode.NORMAL, transition.guidanceMode());
        assertEquals(analysis.availableIngredients(), transition.context().availableIngredients());
    }

    @Test
    void invalidAnswersDoNotAdvanceAndEscalateGuidance() {
        ConversationTransition first = flow.apply(
                ConversationStage.INGREDIENTS,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                0,
                ConversationAnswerAnalysis.invalid()
        );
        ConversationTransition second = flow.apply(
                first.stage(), first.status(), first.context(), first.invalidAnswerCount(),
                ConversationAnswerAnalysis.invalid()
        );
        ConversationTransition third = flow.apply(
                second.stage(), second.status(), second.context(), second.invalidAnswerCount(),
                ConversationAnswerAnalysis.invalid()
        );

        assertEquals(ConversationStage.INGREDIENTS, first.stage());
        assertEquals(ConversationStatus.ACTIVE, first.status());
        assertEquals(1, first.invalidAnswerCount());
        assertEquals(GuidanceMode.EXAMPLE, first.guidanceMode());
        assertEquals(ConversationStage.INGREDIENTS, second.stage());
        assertEquals(2, second.invalidAnswerCount());
        assertEquals(GuidanceMode.QUICK_OPTIONS, second.guidanceMode());
        assertEquals(3, third.invalidAnswerCount());
        assertEquals(GuidanceMode.RESTART_OPTION, third.guidanceMode());
    }

    @Test
    void collectionsAreDefensivelyCopiedAndRemainImmutable() {
        List<AvailableIngredientInput> ingredients = new ArrayList<>(List.of(
                new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)
        ));
        List<String> excluded = new ArrayList<>(List.of("香菜"));
        ConversationAnswerAnalysis analysis = new ConversationAnswerAnalysis(
                true, "清淡", "FAT_LOSS", ingredients, excluded, List.of(),
                30, 1, List.of(), List.of(), new BigDecimal("0.8")
        );
        RecommendationConversationContext context = RecommendationConversationContext.empty().merge(analysis);

        ingredients.clear();
        excluded.add("芹菜");

        assertEquals(1, analysis.availableIngredients().size());
        assertEquals(List.of("香菜"), analysis.excludedIngredients());
        assertEquals(1, context.availableIngredients().size());
        assertNotSame(analysis.availableIngredients(), context.availableIngredients());
        assertThrows(UnsupportedOperationException.class,
                () -> analysis.availableIngredients().add(
                        new AvailableIngredientInput("鸡蛋", null, null, false)));
        assertThrows(UnsupportedOperationException.class, () -> context.excludedIngredients().add("芹菜"));
    }

    @Test
    void validAnswerResetsInvalidCount() {
        ConversationTransition transition = flow.apply(
                ConversationStage.INTENT,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                2,
                new ConversationAnswerAnalysis(
                        true, "清淡饮食", null, List.of(), List.of(), List.of(),
                        null, null, List.of(), List.of(), new BigDecimal("0.9")
                )
        );

        assertEquals(ConversationStage.INGREDIENTS, transition.stage());
        assertEquals(0, transition.invalidAnswerCount());
        assertEquals(GuidanceMode.NORMAL, transition.guidanceMode());
    }

    @Test
    void invalidAnswerGuidanceSaturatesAfterThirdAttempt() {
        ConversationTransition transition = flow.apply(
                ConversationStage.CONTEXT,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                4,
                ConversationAnswerAnalysis.invalid()
        );

        assertEquals(5, transition.invalidAnswerCount());
        assertEquals(GuidanceMode.RESTART_OPTION, transition.guidanceMode());
        assertEquals(ConversationStage.CONTEXT, transition.stage());
    }

    @Test
    void unknownTermsAndConflictsPreventConfirmation() {
        ConversationAnswerAnalysis analysis = completeAnalysis(
                List.of("神秘食材"), List.of("鸡胸肉同时被列为忌口")
        );

        ConversationTransition transition = flow.apply(
                ConversationStage.INTENT,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                0,
                analysis
        );

        assertEquals(ConversationStage.CONFIRM, transition.stage());
        assertEquals(ConversationStatus.ACTIVE, transition.status());
        assertFalse(transition.status() == ConversationStatus.READY_TO_CONFIRM);
        assertEquals(analysis.unknownTerms(), transition.context().unknownTerms());
        assertEquals(analysis.conflicts(), transition.context().conflicts());
    }

    @Test
    void mergeDoesNotClearExistingValuesWithEmptyAnalysis() {
        RecommendationConversationContext existing = RecommendationConversationContext.empty()
                .merge(completeAnalysis(List.of(), List.of()));
        ConversationAnswerAnalysis emptyUpdate = new ConversationAnswerAnalysis(
                true, null, null, List.of(), List.of(), List.of(),
                null, null, List.of(), List.of(), BigDecimal.ONE
        );

        RecommendationConversationContext merged = existing.merge(emptyUpdate);

        assertEquals(existing.intentText(), merged.intentText());
        assertEquals(existing.dietGoal(), merged.dietGoal());
        assertEquals(existing.availableIngredients(), merged.availableIngredients());
        assertEquals(existing.cookingTime(), merged.cookingTime());
        assertEquals(existing.servings(), merged.servings());
        assertEquals(existing.restrictionsConfirmed(), merged.restrictionsConfirmed());
    }

    @Test
    void explicitlyConfirmedEmptyRestrictionsSatisfyRestrictionsStage() {
        RecommendationConversationContext context = new RecommendationConversationContext(
                "清淡饮食", "FAT_LOSS",
                List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
                List.of(), List.of(), 30, 1, List.of(), List.of(), true
        );

        assertEquals(ConversationStage.CONFIRM, flow.firstMissingStage(context));
    }

    private static ConversationAnswerAnalysis completeAnalysis(
            List<String> unknownTerms,
            List<String> conflicts
    ) {
        return new ConversationAnswerAnalysis(
                true,
                "清淡控热量",
                "FAT_LOSS",
                List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
                List.of(),
                List.of(),
                30,
                1,
                unknownTerms,
                conflicts,
                new BigDecimal("0.96")
        );
    }
}
