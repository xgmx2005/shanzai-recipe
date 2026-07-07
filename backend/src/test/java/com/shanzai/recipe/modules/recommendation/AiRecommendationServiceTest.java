package com.shanzai.recipe.modules.recommendation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRecommendationServiceTest {
    @Test
    void fallbackReasonWorksWhenApiUnavailable() {
        AiRecommendationService service = new AiRecommendationService(new DisabledDeepSeekClient());

        String reason = service.generateReason("鸡胸肉西兰花轻食碗", "FAT_LOSS", List.of("鸡胸肉", "西兰花"));

        assertTrue(reason.contains("减脂"));
        assertTrue(reason.contains("鸡胸肉西兰花轻食碗"));
    }

    @Test
    void aiReasonIsUsedWhenClientReturnsText() {
        DeepSeekClient client = (recipeName, dietGoal, matchedIngredients) -> Optional.of(
            new AiRecommendationText("AI推荐理由", "AI健康提示", "AI购物提示")
        );
        AiRecommendationService service = new AiRecommendationService(client);

        String reason = service.generateReason("鸡胸肉西兰花轻食碗", "FAT_LOSS", List.of("鸡胸肉"));

        assertEquals("AI推荐理由", reason);
    }
}
