package com.shanzai.recipe.modules.ingredient;

import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.modules.recipe.RecipeIngredientMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngredientServiceTest {
    private IngredientMapper ingredientMapper;
    private RecipeIngredientMapper recipeIngredientMapper;
    private IngredientService ingredientService;

    @BeforeEach
    void setUp() {
        ingredientMapper = mock(IngredientMapper.class);
        recipeIngredientMapper = mock(RecipeIngredientMapper.class);
        ingredientService = new IngredientService(ingredientMapper, recipeIngredientMapper);
    }

    @Test
    void listIngredientsFiltersByKeywordAliasAndCategory() {
        when(ingredientMapper.selectList(any())).thenReturn(List.of(
            ingredient(1L, "鸡胸肉", "肉蛋奶", "鸡肉,鸡胸"),
            ingredient(2L, "西兰花", "蔬菜", "西蓝花"),
            ingredient(3L, "糙米", "主食", "糙米饭")
        ));

        List<IngredientResponse> responses = ingredientService.listIngredients("鸡肉", "肉蛋奶");

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals("鸡胸肉", responses.get(0).name());
        assertEquals("肉蛋奶", responses.get(0).category());
    }

    @Test
    void createIngredientRejectsDuplicateName() {
        when(ingredientMapper.selectOne(any())).thenReturn(ingredient(1L, "鸡胸肉", "肉蛋奶", "鸡肉"));

        IngredientSaveRequest request = request("鸡胸肉");

        assertThrows(BusinessException.class, () -> ingredientService.createIngredient(request));
        verify(ingredientMapper, never()).insert(any(IngredientEntity.class));
    }

    @Test
    void updateIngredientStoresNutritionFields() {
        when(ingredientMapper.selectById(1L)).thenReturn(ingredient(1L, "鸡胸肉", "肉蛋奶", "鸡肉"));
        when(ingredientMapper.selectOne(any())).thenReturn(null);

        ingredientService.updateIngredient(1L, request("鸡胸肉"));

        ArgumentCaptor<IngredientEntity> captor = ArgumentCaptor.forClass(IngredientEntity.class);
        verify(ingredientMapper).updateById(captor.capture());
        assertEquals(new BigDecimal("24.60"), captor.getValue().getProteinPer100g());
        assertEquals("鸡肉,鸡胸", captor.getValue().getAliases());
    }

    @Test
    void deleteIngredientRejectsIngredientInUse() {
        when(ingredientMapper.selectById(1L)).thenReturn(ingredient(1L, "鸡胸肉", "肉蛋奶", "鸡肉"));
        when(recipeIngredientMapper.selectCount(any())).thenReturn(2L);

        assertThrows(BusinessException.class, () -> ingredientService.deleteIngredient(1L));
        verify(ingredientMapper, never()).deleteById(1L);
    }

    private IngredientSaveRequest request(String name) {
        return new IngredientSaveRequest(
            name,
            "肉蛋奶",
            "g",
            133,
            new BigDecimal("24.60"),
            new BigDecimal("1.90"),
            new BigDecimal("2.50"),
            List.of("鸡肉", "鸡胸")
        );
    }

    private IngredientEntity ingredient(Long id, String name, String category, String aliases) {
        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setId(id);
        ingredient.setName(name);
        ingredient.setCategory(category);
        ingredient.setUnit("g");
        ingredient.setCaloriesPer100g(133);
        ingredient.setProteinPer100g(new BigDecimal("24.60"));
        ingredient.setFatPer100g(new BigDecimal("1.90"));
        ingredient.setCarbsPer100g(new BigDecimal("2.50"));
        ingredient.setAliases(aliases);
        return ingredient;
    }
}
