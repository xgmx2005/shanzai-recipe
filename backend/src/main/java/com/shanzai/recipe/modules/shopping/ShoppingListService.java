package com.shanzai.recipe.modules.shopping;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.modules.ingredient.IngredientEntity;
import com.shanzai.recipe.modules.ingredient.IngredientMapper;
import com.shanzai.recipe.modules.recipe.RecipeEntity;
import com.shanzai.recipe.modules.recipe.RecipeIngredientEntity;
import com.shanzai.recipe.modules.recipe.RecipeIngredientMapper;
import com.shanzai.recipe.modules.recipe.RecipeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ShoppingListService {
    private static final int ACTIVE_RECIPE_STATUS = 1;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";

    private final ShoppingListMapper shoppingListMapper;
    private final ShoppingListItemMapper itemMapper;
    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final IngredientMapper ingredientMapper;
    private final ShoppingListCalculator calculator;

    public ShoppingListService(
        ShoppingListMapper shoppingListMapper,
        ShoppingListItemMapper itemMapper,
        RecipeMapper recipeMapper,
        RecipeIngredientMapper recipeIngredientMapper,
        IngredientMapper ingredientMapper,
        ShoppingListCalculator calculator
    ) {
        this.shoppingListMapper = shoppingListMapper;
        this.itemMapper = itemMapper;
        this.recipeMapper = recipeMapper;
        this.recipeIngredientMapper = recipeIngredientMapper;
        this.ingredientMapper = ingredientMapper;
        this.calculator = calculator;
    }

    @Transactional
    public ShoppingListResponse createShoppingList(Long userId, ShoppingListCreateRequest request) {
        List<Long> recipeIds = cleanIds(request.recipeIds());
        if (recipeIds.isEmpty()) {
            throw new BusinessException("请选择菜谱");
        }
        List<RecipeEntity> recipes = loadActiveRecipes(recipeIds);
        List<RecipeIngredientEntity> rows = recipeIngredientMapper.selectList(
            new LambdaQueryWrapper<RecipeIngredientEntity>().in(RecipeIngredientEntity::getRecipeId, recipeIds)
        );
        Map<Long, IngredientEntity> ingredientsById = ingredientsById(rows);
        List<RecipeNeed> needs = rows.stream()
            .map(row -> toNeed(row, ingredientsById.get(row.getIngredientId())))
            .filter(Objects::nonNull)
            .toList();
        List<ShoppingNeed> shoppingNeeds = calculator.calculate(needs, request.availableIngredients());

        ShoppingListEntity list = new ShoppingListEntity();
        list.setUserId(userId);
        list.setTitle(resolveTitle(request.title(), recipes));
        list.setSourceRecipeIds(joinIds(recipeIds));
        list.setStatus(STATUS_ACTIVE);
        shoppingListMapper.insert(list);

        List<ShoppingListItemEntity> items = shoppingNeeds.stream()
            .map(need -> insertItem(list.getId(), need))
            .toList();
        return toResponse(list, items);
    }

    public List<ShoppingListSummaryResponse> listShoppingLists(Long userId) {
        List<ShoppingListEntity> lists = shoppingListMapper.selectList(
            new LambdaQueryWrapper<ShoppingListEntity>()
                .eq(ShoppingListEntity::getUserId, userId)
                .eq(ShoppingListEntity::getStatus, STATUS_ACTIVE)
                .orderByDesc(ShoppingListEntity::getId)
        );
        return lists.stream()
            .map(list -> {
                List<ShoppingListItemEntity> items = listItems(list.getId());
                int checkedCount = (int) items.stream().filter(item -> Boolean.TRUE.equals(item.getChecked())).count();
                return new ShoppingListSummaryResponse(
                    list.getId(),
                    list.getTitle(),
                    splitIds(list.getSourceRecipeIds()),
                    list.getStatus(),
                    items.size(),
                    checkedCount,
                    list.getCreatedAt()
                );
            })
            .toList();
    }

    public ShoppingListResponse getShoppingList(Long userId, Long id) {
        ShoppingListEntity list = findOwnedActiveList(userId, id);
        return toResponse(list, listItems(list.getId()));
    }

    @Transactional
    public ShoppingListItemResponse updateItemChecked(Long userId, Long listId, Long itemId, Boolean checked) {
        findOwnedActiveList(userId, listId);
        ShoppingListItemEntity item = itemMapper.selectById(itemId);
        if (item == null || !Objects.equals(item.getShoppingListId(), listId)) {
            throw new BusinessException("购物清单项不存在");
        }
        item.setChecked(Boolean.TRUE.equals(checked));
        itemMapper.updateById(item);
        return toItemResponse(item);
    }

    @Transactional
    public void deleteShoppingList(Long userId, Long id) {
        ShoppingListEntity list = findOwnedActiveList(userId, id);
        list.setStatus(STATUS_DELETED);
        shoppingListMapper.updateById(list);
    }

    private List<RecipeEntity> loadActiveRecipes(List<Long> recipeIds) {
        List<RecipeEntity> recipes = recipeMapper.selectBatchIds(recipeIds);
        Map<Long, RecipeEntity> byId = recipes == null
            ? Map.of()
            : recipes.stream()
                .filter(recipe -> Objects.equals(recipe.getStatus(), ACTIVE_RECIPE_STATUS))
                .collect(Collectors.toMap(
                    RecipeEntity::getId,
                    Function.identity(),
                    (first, second) -> first,
                    LinkedHashMap::new
                ));
        if (byId.size() != recipeIds.size()) {
            throw new BusinessException("菜谱不存在或已下架");
        }
        return recipeIds.stream().map(byId::get).toList();
    }

    private Map<Long, IngredientEntity> ingredientsById(List<RecipeIngredientEntity> rows) {
        List<Long> ingredientIds = rows.stream()
            .map(RecipeIngredientEntity::getIngredientId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (ingredientIds.isEmpty()) {
            return Map.of();
        }
        List<IngredientEntity> ingredients = ingredientMapper.selectBatchIds(ingredientIds);
        if (ingredients == null) {
            return Map.of();
        }
        return ingredients.stream()
            .collect(Collectors.toMap(
                IngredientEntity::getId,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
            ));
    }

    private RecipeNeed toNeed(RecipeIngredientEntity row, IngredientEntity ingredient) {
        if (ingredient == null) {
            return null;
        }
        return new RecipeNeed(
            row.getIngredientId(),
            ingredient.getName(),
            ingredient.getCategory(),
            row.getQuantity(),
            row.getUnit()
        );
    }

    private ShoppingListItemEntity insertItem(Long shoppingListId, ShoppingNeed need) {
        ShoppingListItemEntity item = new ShoppingListItemEntity();
        item.setShoppingListId(shoppingListId);
        item.setIngredientId(need.ingredientId());
        item.setIngredientName(need.ingredientName());
        item.setCategory(need.category());
        item.setQuantity(need.quantity());
        item.setUnit(need.unit());
        item.setChecked(false);
        itemMapper.insert(item);
        return item;
    }

    private ShoppingListEntity findOwnedActiveList(Long userId, Long id) {
        ShoppingListEntity list = shoppingListMapper.selectById(id);
        if (list == null
            || !Objects.equals(list.getUserId(), userId)
            || !Objects.equals(list.getStatus(), STATUS_ACTIVE)) {
            throw new BusinessException("购物清单不存在");
        }
        return list;
    }

    private List<ShoppingListItemEntity> listItems(Long listId) {
        return itemMapper.selectList(
            new LambdaQueryWrapper<ShoppingListItemEntity>()
                .eq(ShoppingListItemEntity::getShoppingListId, listId)
                .orderByAsc(ShoppingListItemEntity::getId)
        );
    }

    private ShoppingListResponse toResponse(ShoppingListEntity list, List<ShoppingListItemEntity> items) {
        return new ShoppingListResponse(
            list.getId(),
            list.getTitle(),
            splitIds(list.getSourceRecipeIds()),
            list.getStatus(),
            items.stream().map(this::toItemResponse).toList(),
            list.getCreatedAt(),
            list.getUpdatedAt()
        );
    }

    private ShoppingListItemResponse toItemResponse(ShoppingListItemEntity item) {
        return new ShoppingListItemResponse(
            item.getId(),
            item.getIngredientId(),
            item.getIngredientName(),
            item.getCategory(),
            item.getQuantity(),
            item.getUnit(),
            Boolean.TRUE.equals(item.getChecked())
        );
    }

    private String resolveTitle(String title, List<RecipeEntity> recipes) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        if (recipes.size() == 1) {
            return recipes.get(0).getName() + "采购清单";
        }
        return recipes.size() + "道菜采购清单";
    }

    private List<Long> cleanIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private String joinIds(Collection<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Long> splitIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .map(Long::valueOf)
            .toList();
    }
}
