package com.shanzai.recipe.modules.shopping;

import com.shanzai.recipe.modules.ingredient.IngredientEntity;
import com.shanzai.recipe.modules.ingredient.IngredientMapper;
import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeIngredientEntity;
import com.shanzai.recipe.modules.recipe.RecipeIngredientMapper;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShoppingListServiceTest {
    private ShoppingListMapper shoppingListMapper;
    private ShoppingListItemMapper itemMapper;
    private RecipeMapper recipeMapper;
    private RecipeIngredientMapper recipeIngredientMapper;
    private IngredientMapper ingredientMapper;
    private ShoppingListService shoppingListService;

    @BeforeEach
    void setUp() {
        shoppingListMapper = mock(ShoppingListMapper.class);
        itemMapper = mock(ShoppingListItemMapper.class);
        recipeMapper = mock(RecipeMapper.class);
        recipeIngredientMapper = mock(RecipeIngredientMapper.class);
        ingredientMapper = mock(IngredientMapper.class);
        shoppingListService = new ShoppingListService(
            shoppingListMapper,
            itemMapper,
            recipeMapper,
            recipeIngredientMapper,
            ingredientMapper,
            new ShoppingListCalculator()
        );
    }

    @Test
    void excludesAvailableIngredientsFromShoppingList() {
        ShoppingListCalculator calculator = new ShoppingListCalculator();
        List<RecipeNeed> needs = List.of(
            new RecipeNeed(null, "鸡胸肉", "肉蛋奶", new BigDecimal("200"), "g"),
            new RecipeNeed(null, "西兰花", "蔬菜", new BigDecimal("150"), "g"),
            new RecipeNeed(null, "玉米", "其他", new BigDecimal("80"), "g")
        );

        List<ShoppingNeed> result = calculator.calculate(needs, List.of("鸡胸肉"));

        assertEquals(2, result.size());
        assertEquals("西兰花", result.get(0).ingredientName());
        assertEquals("玉米", result.get(1).ingredientName());
    }

    @Test
    void mergesSameIngredientAndUnitIntoOneShoppingNeed() {
        ShoppingListCalculator calculator = new ShoppingListCalculator();
        List<RecipeNeed> needs = List.of(
            new RecipeNeed(9L, "西兰花", "蔬菜", new BigDecimal("150"), "g"),
            new RecipeNeed(9L, "西兰花", "蔬菜", new BigDecimal("100"), "g")
        );

        List<ShoppingNeed> result = calculator.calculate(needs, List.of());

        assertEquals(1, result.size());
        assertEquals("西兰花", result.get(0).ingredientName());
        assertEquals(new BigDecimal("250"), result.get(0).quantity());
    }

    @Test
    void createsShoppingListFromRecipeNeeds() {
        when(recipeMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(recipe(1L, "鸡胸肉西兰花轻食碗")));
        when(recipeIngredientMapper.selectList(any())).thenReturn(List.of(
            recipeIngredient(1L, 1L, "200", "g"),
            recipeIngredient(1L, 9L, "150", "g"),
            recipeIngredient(1L, 31L, "80", "g")
        ));
        when(ingredientMapper.selectBatchIds(List.of(1L, 9L, 31L))).thenReturn(List.of(
            ingredient(1L, "鸡胸肉", "肉蛋奶"),
            ingredient(9L, "西兰花", "蔬菜"),
            ingredient(31L, "玉米", "其他")
        ));
        when(shoppingListMapper.insert(any(ShoppingListEntity.class))).thenAnswer(invocation -> {
            ShoppingListEntity list = invocation.getArgument(0);
            list.setId(10L);
            return 1;
        });
        AtomicLong itemId = new AtomicLong(20L);
        when(itemMapper.insert(any(ShoppingListItemEntity.class))).thenAnswer(invocation -> {
            ShoppingListItemEntity item = invocation.getArgument(0);
            item.setId(itemId.getAndIncrement());
            return 1;
        });

        ShoppingListResponse response = shoppingListService.createShoppingList(
            7L,
            new ShoppingListCreateRequest(List.of(1L), List.of("鸡胸肉"), null)
        );

        assertEquals(10L, response.id());
        assertEquals("鸡胸肉西兰花轻食碗采购清单", response.title());
        assertEquals(2, response.items().size());
        assertEquals("西兰花", response.items().get(0).ingredientName());
        assertEquals("玉米", response.items().get(1).ingredientName());

        ArgumentCaptor<ShoppingListItemEntity> itemCaptor = ArgumentCaptor.forClass(ShoppingListItemEntity.class);
        verify(itemMapper, times(2)).insert(itemCaptor.capture());
        assertEquals(10L, itemCaptor.getAllValues().get(0).getShoppingListId());
        assertEquals("西兰花", itemCaptor.getAllValues().get(0).getIngredientName());
    }

    private RecipeEntity recipe(Long id, String name) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(id);
        recipe.setName(name);
        recipe.setStatus(1);
        return recipe;
    }

    private RecipeIngredientEntity recipeIngredient(Long recipeId, Long ingredientId, String quantity, String unit) {
        RecipeIngredientEntity row = new RecipeIngredientEntity();
        row.setRecipeId(recipeId);
        row.setIngredientId(ingredientId);
        row.setQuantity(new BigDecimal(quantity));
        row.setUnit(unit);
        return row;
    }

    private IngredientEntity ingredient(Long id, String name, String category) {
        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setId(id);
        ingredient.setName(name);
        ingredient.setCategory(category);
        return ingredient;
    }
}
