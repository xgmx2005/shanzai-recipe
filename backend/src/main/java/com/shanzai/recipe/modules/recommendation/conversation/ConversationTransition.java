package com.shanzai.recipe.modules.recommendation.conversation;

public record ConversationTransition(
        ConversationStage stage,
        ConversationStatus status,
        RecommendationConversationContext context,
        int invalidAnswerCount,
        GuidanceMode guidanceMode
) {
}
