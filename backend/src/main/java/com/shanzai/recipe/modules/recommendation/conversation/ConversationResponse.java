package com.shanzai.recipe.modules.recommendation.conversation;

import java.util.List;

public record ConversationResponse(
        Long id,
        ConversationStage stage,
        ConversationStatus status,
        int invalidAnswerCount,
        RecommendationConversationContext context,
        List<ConversationMessageResponse> messages,
        boolean showConfirmation,
        List<String> quickOptions
) {
}
