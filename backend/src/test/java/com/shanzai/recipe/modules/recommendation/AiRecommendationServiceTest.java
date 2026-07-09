package com.shanzai.recipe.modules.recommendation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRecommendationServiceTest {
    @Test
    void fallbackAnalysisWorksWhenApiUnavailable() {
        AiRecommendationService service = new AiRecommendationService(new DisabledDeepSeekClient());
        AiRecommendationContext context = new AiRecommendationContext(
            "FAT_LOSS",
            List.of("鸡胸肉", "西兰花"),
            List.of("花生"),
            30,
            List.of(new AiRecommendationContext.RecipeSnapshot(
                "鸡胸肉西兰花轻食碗",
                92,
                420,
                "35.00",
                List.of("鸡胸肉", "西兰花"),
                List.of("低脂", "高蛋白")
            ))
        );

        AiRecommendationAnalysis analysis = service.generateAnalysis(context);

        assertEquals(false, analysis.generated());
        assertTrue(analysis.summary().contains("鸡胸肉西兰花轻食碗") || analysis.summary().contains("减脂"));
        assertTrue(analysis.healthTip().contains("建议") || analysis.healthTip().contains("蛋白"));
        assertTrue(analysis.shoppingTip().contains("购物") || analysis.shoppingTip().contains("清单"));
        assertTrue(analysis.topRecipeReason().contains("鸡胸肉西兰花轻食碗"));
    }

    @Test
    void aiAnalysisIsUsedWhenClientReturnsStructuredText() {
        DeepSeekClient client = context -> java.util.Optional.of(
            new AiRecommendationText("AI总结", "AI健康提示", "AI购物提示", "AI推荐理由")
        );
        AiRecommendationService service = new AiRecommendationService(client);
        AiRecommendationContext context = new AiRecommendationContext(
            "BALANCED",
            List.of("番茄", "鸡蛋"),
            List.of(),
            20,
            List.of(new AiRecommendationContext.RecipeSnapshot(
                "番茄炒蛋",
                88,
                320,
                "18.00",
                List.of("番茄", "鸡蛋"),
                List.of("家常", "快手")
            ))
        );

        AiRecommendationAnalysis analysis = service.generateAnalysis(context);

        assertEquals(true, analysis.generated());
        assertEquals("AI总结", analysis.summary());
        assertEquals("AI健康提示", analysis.healthTip());
        assertEquals("AI购物提示", analysis.shoppingTip());
        assertEquals("AI推荐理由", analysis.topRecipeReason());
    }
}
