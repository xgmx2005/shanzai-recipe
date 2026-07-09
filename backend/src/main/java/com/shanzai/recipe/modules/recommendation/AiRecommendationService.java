package com.shanzai.recipe.modules.recommendation;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AiRecommendationService {
    private final DeepSeekClient deepSeekClient;

    public AiRecommendationService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public AiRecommendationAnalysis generateAnalysis(AiRecommendationContext context) {
        return deepSeekClient.generateRecommendationText(context)
            .filter(this::complete)
            .map(text -> new AiRecommendationAnalysis(
                text.summary().trim(),
                text.healthTip().trim(),
                text.shoppingTip().trim(),
                text.topRecipeReason().trim(),
                true
            ))
            .orElseGet(() -> fallbackAnalysis(context));
    }

    public String generateLocalReason(String recipeName, String dietGoal, List<String> matchedIngredients) {
        return fallbackReason(recipeName, dietGoal, matchedIngredients);
    }

    private boolean complete(AiRecommendationText text) {
        return text != null
            && hasText(text.summary())
            && hasText(text.healthTip())
            && hasText(text.shoppingTip())
            && hasText(text.topRecipeReason());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private AiRecommendationAnalysis fallbackAnalysis(AiRecommendationContext context) {
        List<AiRecommendationContext.RecipeSnapshot> recipes = context == null || context.recipes() == null
            ? List.of()
            : context.recipes();
        if (recipes.isEmpty()) {
            return new AiRecommendationAnalysis(
                "暂未找到完全匹配的菜谱，建议放宽食材或时间条件后重新推荐。",
                "建议先保留核心健康目标，再适当放宽烹饪时间或食材条件。",
                "当前没有可生成购物清单的菜谱，建议重新输入已有食材后再尝试。",
                "暂无可推荐菜谱。",
                false
            );
        }

        AiRecommendationContext.RecipeSnapshot topRecipe = recipes.get(0);
        String goal = context == null ? "BALANCED" : String.valueOf(context.dietGoal());
        String available = join(context == null ? List.of() : context.availableIngredients(), "现有食材");
        String summary = switch (goal) {
            case "FAT_LOSS" -> "本次优先推荐" + topRecipe.name() + "，结合" + available + "，更适合减脂控热量场景。";
            case "MUSCLE_GAIN" -> "本次优先推荐" + topRecipe.name() + "，结合" + available + "，更适合健身增肌补充蛋白。";
            default -> "本次优先推荐" + topRecipe.name() + "，结合" + available + "，适合日常均衡饮食。";
        };
        String healthTip = switch (goal) {
            case "FAT_LOSS" -> "建议控制额外油脂摄入，保留高蛋白食材，并搭配蔬菜增强饱腹感。";
            case "MUSCLE_GAIN" -> "建议保证主食和蛋白质同时摄入，训练后可优先选择蛋白质更高的菜谱。";
            default -> "建议保持主食、蛋白质和蔬菜搭配，避免单一食材造成营养不均衡。";
        };
        String shoppingTip = "生成购物清单时会自动排除你已有的" + available + "，只补充菜谱中缺少的食材。";
        return new AiRecommendationAnalysis(
            summary,
            healthTip,
            shoppingTip,
            fallbackReason(topRecipe.name(), goal, topRecipe.matchedIngredients()),
            false
        );
    }

    private String join(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        String joined = values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .reduce((first, second) -> first + "、" + second)
            .orElse("");
        return joined.isBlank() ? fallback : joined;
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
