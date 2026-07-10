package com.shanzai.recipe.modules.recommendation.conversation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryConversationAnswerInterpreterTest {
    private final DictionaryConversationAnswerInterpreter interpreter =
            new DictionaryConversationAnswerInterpreter();

    @Test
    void normalizesFoodAliasesAndExtractsContextNumbers() {
        ConversationAnswerAnalysis analysis = interpreter.interpret(
                ConversationStage.INGREDIENTS,
                "有300克鸡胸肉、两个鸡蛋和西蓝花，一个人吃，半小时以内",
                RecommendationConversationContext.empty()
        );

        assertTrue(analysis.relevant());
        assertEquals(List.of("鸡胸肉", "鸡蛋", "西兰花"),
                analysis.availableIngredients().stream().map(AvailableIngredientInput::name).toList());
        assertEquals(new BigDecimal("300"), analysis.availableIngredients().get(0).quantity());
        assertEquals("g", analysis.availableIngredients().get(0).unit());
        assertEquals(new BigDecimal("2"), analysis.availableIngredients().get(1).quantity());
        assertEquals("个", analysis.availableIngredients().get(1).unit());
        assertEquals(1, analysis.servings());
        assertEquals(30, analysis.cookingTime());
    }

    @Test
    void rejectsSymbolsWithoutChangingContext() {
        RecommendationConversationContext context = new RecommendationConversationContext(
                "清淡饮食", "FAT_LOSS",
                List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
                List.of(), List.of(), 30, 1, List.of("肉"), List.of(), false
        );

        ConversationAnswerAnalysis analysis = interpreter.interpret(
                ConversationStage.INGREDIENTS, "@@@？？？", context);

        assertFalse(analysis.relevant());
        assertEquals(context, context.merge(analysis));
    }

    @Test
    void rejectsUnrelatedWordsWithoutChangingContext() {
        RecommendationConversationContext context = RecommendationConversationContext.empty();

        ConversationAnswerAnalysis analysis = interpreter.interpret(
                ConversationStage.INTENT, "今天天气不错", context);

        assertFalse(analysis.relevant());
        assertEquals(context, context.merge(analysis));
    }

    @Test
    void isolatesVagueTermsAndMarksOnlyExplicitRestrictionAnswers() {
        ConversationAnswerAnalysis vague = interpreter.interpret(
                ConversationStage.INGREDIENTS, "有肉和菜", RecommendationConversationContext.empty());
        ConversationAnswerAnalysis timeOnly = interpreter.interpret(
                ConversationStage.RESTRICTIONS, "30分钟", RecommendationConversationContext.empty());
        ConversationAnswerAnalysis unrestricted = interpreter.interpret(
                ConversationStage.RESTRICTIONS, "没有忌口，都可以吃", RecommendationConversationContext.empty());
        ConversationAnswerAnalysis allergy = interpreter.interpret(
                ConversationStage.RESTRICTIONS, "花生过敏，不吃香菜", RecommendationConversationContext.empty());

        assertTrue(vague.availableIngredients().isEmpty());
        assertEquals(List.of("肉", "菜"), vague.unknownTerms());
        assertFalse(timeOnly.restrictionsAnswered());
        assertTrue(unrestricted.restrictionsAnswered());
        assertTrue(unrestricted.excludedIngredients().isEmpty());
        assertTrue(unrestricted.allergyIngredients().isEmpty());
        assertTrue(allergy.restrictionsAnswered());
        assertEquals(List.of("香菜"), allergy.excludedIngredients());
        assertEquals(List.of("花生"), allergy.allergyIngredients());
    }

    @Test
    void rejectsInvalidQuantityAndClearsResolvedIssues() {
        ConversationAnswerAnalysis invalidQuantity = interpreter.interpret(
                ConversationStage.INGREDIENTS, "0克鸡胸肉", RecommendationConversationContext.empty());
        RecommendationConversationContext blocked = new RecommendationConversationContext(
                "清淡饮食", null, List.of(), List.of(), List.of(), null, null,
                List.of("肉"), List.of(), false
        );
        ConversationAnswerAnalysis clarification = interpreter.interpret(
                ConversationStage.INGREDIENTS, "肉是鸡胸肉", blocked);

        assertTrue(invalidQuantity.availableIngredients().isEmpty());
        assertTrue(invalidQuantity.conflicts().stream().anyMatch(value -> value.contains("鸡胸肉")));
        assertEquals(List.of("鸡胸肉"),
                clarification.availableIngredients().stream().map(AvailableIngredientInput::name).toList());
        assertTrue(clarification.unknownTerms().isEmpty());
        assertTrue(blocked.merge(clarification).unknownTerms().isEmpty());
    }

    @Test
    void capturesSpecificRestrictionWordsOutsideFoodAliasDictionary() {
        ConversationAnswerAnalysis spicy = interpreter.interpret(
                ConversationStage.RESTRICTIONS, "不吃辣", RecommendationConversationContext.empty());
        ConversationAnswerAnalysis seafoodAllergy = interpreter.interpret(
                ConversationStage.RESTRICTIONS, "海鲜过敏", RecommendationConversationContext.empty());
        ConversationAnswerAnalysis lowSalt = interpreter.interpret(
                ConversationStage.RESTRICTIONS, "低盐", RecommendationConversationContext.empty());
        ConversationAnswerAnalysis bounded = interpreter.interpret(
                ConversationStage.RESTRICTIONS, "不吃辣和海鲜，半小时内", RecommendationConversationContext.empty());

        assertTrue(spicy.relevant());
        assertTrue(spicy.restrictionsAnswered());
        assertEquals(List.of("辣"), spicy.excludedIngredients());
        assertTrue(spicy.availableIngredients().isEmpty());

        assertTrue(seafoodAllergy.restrictionsAnswered());
        assertEquals(List.of("海鲜"), seafoodAllergy.allergyIngredients());
        assertTrue(seafoodAllergy.availableIngredients().isEmpty());

        assertTrue(lowSalt.restrictionsAnswered());
        assertTrue(lowSalt.excludedIngredients().isEmpty());
        assertTrue(lowSalt.availableIngredients().isEmpty());

        assertEquals(List.of("辣", "海鲜"), bounded.excludedIngredients());
        assertEquals(30, bounded.cookingTime());
        assertTrue(bounded.availableIngredients().isEmpty());
    }
}
