package com.shanzai.recipe.modules.ingredient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.modules.recipe.RecipeIngredientEntity;
import com.shanzai.recipe.modules.recipe.RecipeIngredientMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class IngredientService {
    private final IngredientMapper ingredientMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;

    public IngredientService(IngredientMapper ingredientMapper, RecipeIngredientMapper recipeIngredientMapper) {
        this.ingredientMapper = ingredientMapper;
        this.recipeIngredientMapper = recipeIngredientMapper;
    }

    public List<IngredientResponse> listIngredients(String keyword, String category) {
        return ingredientMapper.selectList(new LambdaQueryWrapper<IngredientEntity>().orderByAsc(IngredientEntity::getId))
            .stream()
            .filter(ingredient -> matchesKeyword(ingredient, keyword))
            .filter(ingredient -> matchesCategory(ingredient, category))
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public IngredientResponse createIngredient(IngredientSaveRequest request) {
        String name = cleanRequired(request.name(), "食材名称不能为空");
        ensureNameAvailable(name, null);

        IngredientEntity ingredient = new IngredientEntity();
        applyRequest(ingredient, request);
        ingredientMapper.insert(ingredient);
        return toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse updateIngredient(Long id, IngredientSaveRequest request) {
        IngredientEntity ingredient = findExisting(id);
        String name = cleanRequired(request.name(), "食材名称不能为空");
        ensureNameAvailable(name, id);

        applyRequest(ingredient, request);
        ingredientMapper.updateById(ingredient);
        return toResponse(ingredient);
    }

    @Transactional
    public void deleteIngredient(Long id) {
        IngredientEntity ingredient = findExisting(id);
        Long usedCount = recipeIngredientMapper.selectCount(
            new LambdaQueryWrapper<RecipeIngredientEntity>().eq(RecipeIngredientEntity::getIngredientId, ingredient.getId())
        );
        if (usedCount != null && usedCount > 0) {
            throw new BusinessException("该食材已被菜谱使用，不能删除");
        }
        ingredientMapper.deleteById(id);
    }

    private IngredientEntity findExisting(Long id) {
        IngredientEntity ingredient = ingredientMapper.selectById(id);
        if (ingredient == null) {
            throw new BusinessException("食材不存在");
        }
        return ingredient;
    }

    private void ensureNameAvailable(String name, Long currentId) {
        IngredientEntity existing = ingredientMapper.selectOne(
            new LambdaQueryWrapper<IngredientEntity>().eq(IngredientEntity::getName, name)
        );
        if (existing != null && !Objects.equals(existing.getId(), currentId)) {
            throw new BusinessException("食材名称已存在");
        }
    }

    private void applyRequest(IngredientEntity ingredient, IngredientSaveRequest request) {
        ingredient.setName(cleanRequired(request.name(), "食材名称不能为空"));
        ingredient.setCategory(cleanRequired(request.category(), "食材分类不能为空"));
        ingredient.setUnit(cleanRequired(request.unit(), "计量单位不能为空"));
        ingredient.setCaloriesPer100g(request.caloriesPer100g());
        ingredient.setProteinPer100g(request.proteinPer100g());
        ingredient.setFatPer100g(request.fatPer100g());
        ingredient.setCarbsPer100g(request.carbsPer100g());
        ingredient.setAliases(joinList(request.aliases()));
    }

    private IngredientResponse toResponse(IngredientEntity ingredient) {
        return new IngredientResponse(
            ingredient.getId(),
            ingredient.getName(),
            ingredient.getCategory(),
            ingredient.getUnit(),
            ingredient.getCaloriesPer100g(),
            ingredient.getProteinPer100g(),
            ingredient.getFatPer100g(),
            ingredient.getCarbsPer100g(),
            splitList(ingredient.getAliases()),
            ingredient.getCreatedAt(),
            ingredient.getUpdatedAt()
        );
    }

    private boolean matchesKeyword(IngredientEntity ingredient, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim();
        return contains(ingredient.getName(), normalized) || contains(ingredient.getAliases(), normalized);
    }

    private boolean matchesCategory(IngredientEntity ingredient, String category) {
        return category == null || category.isBlank() || category.trim().equals(ingredient.getCategory());
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private String cleanRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .collect(Collectors.joining(","));
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .toList();
    }
}
