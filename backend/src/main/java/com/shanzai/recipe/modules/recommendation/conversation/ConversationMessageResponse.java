package com.shanzai.recipe.modules.recommendation.conversation;

import java.time.LocalDateTime;

public record ConversationMessageResponse(
        Long id,
        String role,
        String content,
        String clientMessageId,
        LocalDateTime createdAt
) {
}
