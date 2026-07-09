package com.shanzai.recipe.modules.recommendation;

import java.util.Optional;

@FunctionalInterface
public interface DeepSeekClient {
    Optional<AiRecommendationText> generateRecommendationText(AiRecommendationContext context);
}
