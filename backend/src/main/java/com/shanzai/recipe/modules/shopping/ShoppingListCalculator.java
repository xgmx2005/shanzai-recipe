package com.shanzai.recipe.modules.shopping;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class ShoppingListCalculator {
    public List<ShoppingNeed> calculate(List<RecipeNeed> needs, List<String> availableIngredients) {
        Set<String> available = normalizedSet(availableIngredients);
        Map<String, ShoppingNeed> merged = new LinkedHashMap<>();
        safeNeeds(needs).stream()
            .filter(need -> !available.contains(normalize(need.ingredientName())))
            .forEach(need -> mergeNeed(merged, need));
        return List.copyOf(merged.values());
    }

    private void mergeNeed(Map<String, ShoppingNeed> merged, RecipeNeed need) {
        String key = normalize(need.ingredientName()) + "\u0000" + normalize(need.unit());
        ShoppingNeed current = merged.get(key);
        if (current == null) {
            merged.put(key, new ShoppingNeed(
                need.ingredientId(),
                need.ingredientName(),
                need.category(),
                defaultQuantity(need.quantity()),
                need.unit()
            ));
            return;
        }
        merged.put(key, new ShoppingNeed(
            current.ingredientId(),
            current.ingredientName(),
            current.category(),
            current.quantity().add(defaultQuantity(need.quantity())),
            current.unit()
        ));
    }

    private List<RecipeNeed> safeNeeds(List<RecipeNeed> needs) {
        if (needs == null || needs.isEmpty()) {
            return List.of();
        }
        return needs.stream()
            .filter(Objects::nonNull)
            .filter(need -> !normalize(need.ingredientName()).isBlank())
            .toList();
    }

    private Set<String> normalizedSet(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        values.stream()
            .filter(Objects::nonNull)
            .map(this::normalize)
            .filter(value -> !value.isBlank())
            .forEach(normalized::add);
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
