package com.shanzai.recipe.modules.recommendation.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationMessageRequest(
        @NotBlank @Size(max = 1000) String content,
        @NotBlank @Size(max = 64) String clientMessageId
) {
}
