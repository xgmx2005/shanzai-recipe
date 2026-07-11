package com.shanzai.recipe.modules.recommendation.conversation;

public record ConversationMessageRequest(
        String clientMessageId,
        String content
) {
}
