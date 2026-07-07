package com.shanzai.recipe.modules.recommendation;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiRecommendationService {
    private final DeepSeekClient deepSeekClient;

    public AiRecommendationService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public String generateSummary(String dietGoal, List<RecommendedRecipeResponse> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return "暂未找到完全匹配的菜谱，建议放宽食材或时间条件后重新推荐。";
        }
        return switch (String.valueOf(dietGoal)) {
            case "FAT_LOSS" -> "今天推荐低脂高蛋白搭配，适合控制热量。";
            case "MUSCLE_GAIN" -> "今天推荐高蛋白组合，适合训练后补充能量。";
            default -> "今天推荐营养均衡的搭配，适合日常健康饮食。";
        };
    }

    public String generateReason(String recipeName, String dietGoal, List<String> matchedIngredients) {
        return deepSeekClient.generateRecommendationText(recipeName, dietGoal, matchedIngredients)
            .map(AiRecommendationText::reason)
            .filter(reason -> !reason.isBlank())
            .orElseGet(() -> fallbackReason(recipeName, dietGoal, matchedIngredients));
    }

    private String fallbackReason(String recipeName, String dietGoal, List<String> matchedIngredients) {
        String ingredients = matchedIngredients == null || matchedIngredients.isEmpty()
            ? "现有食材"
            : String.join("、", matchedIngredients);
        return switch (String.valueOf(dietGoal)) {
            case "FAT_LOSS" -> recipeName + "适合减脂期，" + ingredients + "匹配度高，整体热量更容易控制。";
            case "MUSCLE_GAIN" -> recipeName + "蛋白质补充更充分，" + ingredients + "可以直接利用，适合增肌餐。";
            default -> recipeName + "搭配均衡，" + ingredients + "匹配度高，适合日常健康饮食。";
        };
    }
}
