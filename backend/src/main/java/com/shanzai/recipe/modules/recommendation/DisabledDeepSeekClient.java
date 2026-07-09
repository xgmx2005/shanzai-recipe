package com.shanzai.recipe.modules.recommendation;

import java.util.Optional;

public class DisabledDeepSeekClient implements DeepSeekClient {
    @Override
    public Optional<AiRecommendationText> generateRecommendationText(AiRecommendationContext context) {
        return Optional.empty();
    }
}
