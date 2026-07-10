package com.shanzai.recipe.modules.recommendation.conversation;

public interface ConversationAnswerInterpreter {
    ConversationAnswerAnalysis interpret(
            ConversationStage stage,
            String content,
            RecommendationConversationContext context
    );
}
