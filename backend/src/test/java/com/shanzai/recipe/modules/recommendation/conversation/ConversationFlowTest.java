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
        ConversationAnswerAnalysis analysis = completeAnalysis(true, List.of(), List.of());

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
        assertFalse(ConversationAnswerAnalysis.invalid().restrictionsAnswered());

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
    void intentTextAloneCompletesIntentStage() {
        ConversationTransition transition = flow.apply(
                ConversationStage.INTENT,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                0,
                new ConversationAnswerAnalysis(
                        true, "清淡饮食", null, List.of(), List.of(), List.of(),
                        null, null, List.of(), List.of(), BigDecimal.ONE
                )
        );

        assertEquals(ConversationStage.INGREDIENTS, transition.stage());
    }

    @Test
    void relevantContextAnswerDoesNotConfirmRestrictionsWithoutExplicitSignal() {
        RecommendationConversationContext context = contextBeforeRestrictions();
        ConversationAnswerAnalysis analysis = new ConversationAnswerAnalysis(
                true, null, null, List.of(), List.of(), List.of(),
                30, null, List.of(), List.of(), BigDecimal.ONE, false
        );

        ConversationTransition transition = flow.apply(
                ConversationStage.RESTRICTIONS,
                ConversationStatus.ACTIVE,
                context,
                0,
                analysis
        );

        assertEquals(ConversationStage.RESTRICTIONS, transition.stage());
        assertFalse(transition.context().restrictionsConfirmed());
        assertEquals(30, transition.context().cookingTime());
    }

    @Test
    void completeAnalysisWithoutExplicitRestrictionsAnswerCannotConfirm() {
        ConversationTransition transition = flow.apply(
                ConversationStage.INTENT,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                0,
                completeAnalysis(false, List.of(), List.of())
        );

        assertEquals(ConversationStage.RESTRICTIONS, transition.stage());
        assertEquals(ConversationStatus.ACTIVE, transition.status());
        assertFalse(transition.context().restrictionsConfirmed());
    }

    @Test
    void explicitRestrictionsAnswerAllowsConfirmation() {
        ConversationTransition transition = flow.apply(
                ConversationStage.INTENT,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                0,
                completeAnalysis(true, List.of(), List.of())
        );

        assertEquals(ConversationStage.CONFIRM, transition.stage());
        assertEquals(ConversationStatus.READY_TO_CONFIRM, transition.status());
        assertTrue(transition.context().restrictionsConfirmed());
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
    void invalidAnswerCountSaturatesAtIntegerMaximum() {
        ConversationTransition transition = flow.apply(
                ConversationStage.CONTEXT,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                Integer.MAX_VALUE,
                ConversationAnswerAnalysis.invalid()
        );

        assertEquals(Integer.MAX_VALUE, transition.invalidAnswerCount());
        assertEquals(GuidanceMode.RESTART_OPTION, transition.guidanceMode());
    }

    @Test
    void unknownTermsAndConflictsPreventConfirmation() {
        ConversationAnswerAnalysis analysis = completeAnalysis(
                true,
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
    void resolvedIssuesReplacePreviousUnknownTermsAndConflicts() {
        ConversationTransition unresolved = flow.apply(
                ConversationStage.INTENT,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                0,
                completeAnalysis(true, List.of("神秘食材"), List.of("冲突条件"))
        );
        ConversationAnswerAnalysis correction = new ConversationAnswerAnalysis(
                true, null, null, List.of(), List.of(), List.of(),
                null, null, List.of(), List.of(), BigDecimal.ONE
        );

        ConversationTransition resolved = flow.apply(
                unresolved.stage(),
                unresolved.status(),
                unresolved.context(),
                unresolved.invalidAnswerCount(),
                correction
        );

        assertTrue(resolved.context().unknownTerms().isEmpty());
        assertTrue(resolved.context().conflicts().isEmpty());
        assertEquals(ConversationStage.CONFIRM, resolved.stage());
        assertEquals(ConversationStatus.READY_TO_CONFIRM, resolved.status());
    }

    @Test
    void nullStageOrStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> flow.apply(
                null, ConversationStatus.ACTIVE, null, 0, ConversationAnswerAnalysis.invalid()));
        assertThrows(IllegalArgumentException.class, () -> flow.apply(
                ConversationStage.INTENT, null, null, 0, ConversationAnswerAnalysis.invalid()));
    }

    @Test
    void completedOrCancelledConversationCannotMigrate() {
        assertThrows(IllegalStateException.class, () -> flow.apply(
                ConversationStage.CONFIRM, ConversationStatus.COMPLETED,
                RecommendationConversationContext.empty(), 0, ConversationAnswerAnalysis.invalid()));
        assertThrows(IllegalStateException.class, () -> flow.apply(
                ConversationStage.CONFIRM, ConversationStatus.CANCELLED,
                RecommendationConversationContext.empty(), 0, ConversationAnswerAnalysis.invalid()));
    }

    @Test
    void readyToConfirmCanBeEditedButIssuesKeepItActive() {
        ConversationTransition ready = flow.apply(
                ConversationStage.INTENT,
                ConversationStatus.ACTIVE,
                RecommendationConversationContext.empty(),
                0,
                completeAnalysis(true, List.of(), List.of())
        );

        ConversationTransition edited = flow.apply(
                ready.stage(),
                ready.status(),
                ready.context(),
                ready.invalidAnswerCount(),
                completeAnalysis(true, List.of("待澄清词"), List.of())
        );

        assertEquals(ConversationStage.CONFIRM, edited.stage());
        assertEquals(ConversationStatus.ACTIVE, edited.status());
        assertEquals(List.of("待澄清词"), edited.context().unknownTerms());
    }

    @Test
    void sameIngredientNameUsesLatestQuantityAndKeepsOneEntry() {
        RecommendationConversationContext first = RecommendationConversationContext.empty().merge(
                new ConversationAnswerAnalysis(
                        true, "清淡", null,
                        List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
                        List.of(), List.of(), null, null, List.of(), List.of(), BigDecimal.ONE
                ));

        RecommendationConversationContext merged = first.merge(
                new ConversationAnswerAnalysis(
                        true, null, null,
                        List.of(new AvailableIngredientInput(" 鸡胸肉 ", new BigDecimal("500"), "g", true)),
                        List.of(), List.of(), null, null, List.of(), List.of(), BigDecimal.ONE
                ));

        assertEquals(1, merged.availableIngredients().size());
        assertEquals(new BigDecimal("500"), merged.availableIngredients().get(0).quantity());
    }

    @Test
    void laterRestrictionRemovesMatchingIngredientFromMergedContext() {
        RecommendationConversationContext existing = new RecommendationConversationContext(
                "清淡饮食", null,
                List.of(new AvailableIngredientInput("虾", new BigDecimal("200"), "g", true)),
                List.of(), List.of(), 30, 1, List.of(), List.of(), false
        );
        ConversationAnswerAnalysis restriction = new ConversationAnswerAnalysis(
                true, null, null, List.of(), List.of(), List.of("虾"),
                null, null, List.of(), List.of(), BigDecimal.ONE, true
        );

        RecommendationConversationContext merged = existing.merge(restriction);

        assertTrue(merged.availableIngredients().isEmpty());
        assertEquals(List.of("虾"), merged.allergyIngredients());
    }

    @Test
    void mergeDoesNotClearExistingValuesWithEmptyAnalysis() {
        RecommendationConversationContext existing = RecommendationConversationContext.empty()
                .merge(completeAnalysis(true, List.of(), List.of()));
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

    private static RecommendationConversationContext contextBeforeRestrictions() {
        return new RecommendationConversationContext(
                "清淡饮食", "FAT_LOSS",
                List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
                List.of(), List.of(), null, null, List.of(), List.of(), false
        );
    }

    private static ConversationAnswerAnalysis completeAnalysis(
            boolean restrictionsAnswered,
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
                new BigDecimal("0.96"),
                restrictionsAnswered
        );
    }
}
