package com.shanzai.recipe.modules.recipe;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.common.DietGoal;
import com.shanzai.recipe.modules.ingredient.IngredientEntity;
import com.shanzai.recipe.modules.ingredient.IngredientMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_INACTIVE = 0;

    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final IngredientMapper ingredientMapper;
    private final ObjectMapper objectMapper;

    public RecipeService(
        RecipeMapper recipeMapper,
        RecipeIngredientMapper recipeIngredientMapper,
        IngredientMapper ingredientMapper,
        ObjectMapper objectMapper
    ) {
        this.recipeMapper = recipeMapper;
        this.recipeIngredientMapper = recipeIngredientMapper;
        this.ingredientMapper = ingredientMapper;
        this.objectMapper = objectMapper;
    }

    public List<RecipeSummaryResponse> listRecipes(String keyword, DietGoal dietGoal, String tag) {
        return recipeMapper.selectList(new LambdaQueryWrapper<RecipeEntity>().orderByAsc(RecipeEntity::getId))
            .stream()
            .filter(recipe -> Objects.equals(recipe.getStatus(), STATUS_ACTIVE))
            .filter(recipe -> matchesKeyword(recipe, keyword))
            .filter(recipe -> matchesDietGoal(recipe, dietGoal))
            .filter(recipe -> matchesTag(recipe, tag))
            .map(this::toSummary)
            .toList();
    }

    public List<RecipeSummaryResponse> listAdminRecipes(String keyword, DietGoal dietGoal, String tag, Integer status) {
        return recipeMapper.selectList(new LambdaQueryWrapper<RecipeEntity>().orderByAsc(RecipeEntity::getId))
            .stream()
            .filter(recipe -> status == null || Objects.equals(recipe.getStatus(), status))
            .filter(recipe -> matchesKeyword(recipe, keyword))
            .filter(recipe -> matchesDietGoal(recipe, dietGoal))
            .filter(recipe -> matchesTag(recipe, tag))
            .map(this::toSummary)
            .toList();
    }

    public RecipeDetailResponse getRecipeDetail(Long id) {
        RecipeEntity recipe = findRecipe(id);
        if (!Objects.equals(recipe.getStatus(), STATUS_ACTIVE)) {
            throw new BusinessException("菜谱不存在或已下架");
        }
        return toDetail(recipe);
    }

    public RecipeDetailResponse getAdminRecipeDetail(Long id) {
        return toDetail(findRecipe(id));
    }

    @Transactional
    public RecipeDetailResponse createRecipe(Long maintainerId, RecipeSaveRequest request) {
        RecipeEntity recipe = new RecipeEntity();
        applyRequest(recipe, request);
        recipe.setStatus(STATUS_ACTIVE);
        recipe.setCreatedBy(maintainerId);
        recipeMapper.insert(recipe);

        Map<Long, IngredientEntity> ingredients = ingredientsById(request.ingredients());
        List<RecipeIngredientEntity> rows = buildRecipeIngredients(recipe.getId(), request.ingredients());
        rows.forEach(recipeIngredientMapper::insert);
        return toDetail(recipe, rows, ingredients);
    }

    @Transactional
    public RecipeDetailResponse updateRecipe(Long id, RecipeSaveRequest request) {
        RecipeEntity recipe = findRecipe(id);
        applyRequest(recipe, request);
        recipeMapper.updateById(recipe);

        recipeIngredientMapper.delete(
            new LambdaQueryWrapper<RecipeIngredientEntity>().eq(RecipeIngredientEntity::getRecipeId, recipe.getId())
        );
        Map<Long, IngredientEntity> ingredients = ingredientsById(request.ingredients());
        List<RecipeIngredientEntity> rows = buildRecipeIngredients(recipe.getId(), request.ingredients());
        rows.forEach(recipeIngredientMapper::insert);
        return toDetail(recipe, rows, ingredients);
    }

    @Transactional
    public void deleteRecipe(Long id) {
        RecipeEntity recipe = findRecipe(id);
        recipe.setStatus(STATUS_INACTIVE);
        recipeMapper.updateById(recipe);
    }

    private RecipeEntity findRecipe(Long id) {
        RecipeEntity recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BusinessException("菜谱不存在");
        }
        return recipe;
    }

    private void applyRequest(RecipeEntity recipe, RecipeSaveRequest request) {
        recipe.setName(cleanRequired(request.name(), "菜谱名称不能为空"));
        recipe.setDescription(cleanRequired(request.description(), "菜谱描述不能为空"));
        recipe.setImageUrl(cleanText(request.imageUrl()));
        recipe.setCookingTime(request.cookingTime());
        recipe.setDifficulty(cleanRequired(request.difficulty(), "难度不能为空"));
        recipe.setServings(request.servings());
        recipe.setCalories(defaultInteger(request.calories()));
        recipe.setProtein(defaultDecimal(request.protein()));
        recipe.setFat(defaultDecimal(request.fat()));
        recipe.setCarbs(defaultDecimal(request.carbs()));
        recipe.setTasteTags(joinList(request.tasteTags()));
        recipe.setHealthTags(joinList(request.healthTags()));
        recipe.setTargetGoals(joinGoals(request.targetGoals()));
        recipe.setSteps(toStepsJson(request.steps()));
    }

    private RecipeDetailResponse toDetail(RecipeEntity recipe) {
        List<RecipeIngredientEntity> rows = recipeIngredientMapper.selectList(
            new LambdaQueryWrapper<RecipeIngredientEntity>().eq(RecipeIngredientEntity::getRecipeId, recipe.getId())
        );
        Map<Long, IngredientEntity> ingredients = ingredientsById(rows.stream()
            .map(RecipeIngredientEntity::getIngredientId)
            .toList());
        return toDetail(recipe, rows, ingredients);
    }

    private RecipeDetailResponse toDetail(
        RecipeEntity recipe,
        List<RecipeIngredientEntity> rows,
        Map<Long, IngredientEntity> ingredients
    ) {
        return new RecipeDetailResponse(
            recipe.getId(),
            recipe.getName(),
            recipe.getDescription(),
            recipe.getImageUrl(),
            recipe.getCookingTime(),
            recipe.getDifficulty(),
            recipe.getServings(),
            recipe.getCalories(),
            recipe.getProtein(),
            recipe.getFat(),
            recipe.getCarbs(),
            splitList(recipe.getTasteTags()),
            splitList(recipe.getHealthTags()),
            splitList(recipe.getTargetGoals()),
            enrichSteps(parseSteps(recipe.getSteps())),
            rows.stream()
                .map(row -> toIngredientResponse(row, ingredients.get(row.getIngredientId())))
                .toList(),
            recipe.getStatus(),
            recipe.getCreatedBy(),
            recipe.getCreatedAt(),
            recipe.getUpdatedAt()
        );
    }

    private RecipeSummaryResponse toSummary(RecipeEntity recipe) {
        return new RecipeSummaryResponse(
            recipe.getId(),
            recipe.getName(),
            recipe.getDescription(),
            recipe.getImageUrl(),
            recipe.getCookingTime(),
            recipe.getDifficulty(),
            recipe.getServings(),
            recipe.getCalories(),
            recipe.getProtein(),
            recipe.getFat(),
            recipe.getCarbs(),
            splitList(recipe.getTasteTags()),
            splitList(recipe.getHealthTags()),
            splitList(recipe.getTargetGoals()),
            recipe.getStatus()
        );
    }

    private RecipeIngredientResponse toIngredientResponse(RecipeIngredientEntity row, IngredientEntity ingredient) {
        return new RecipeIngredientResponse(
            row.getIngredientId(),
            ingredient == null ? "" : ingredient.getName(),
            ingredient == null ? "" : ingredient.getCategory(),
            row.getQuantity(),
            row.getUnit(),
            Boolean.TRUE.equals(row.getCore())
        );
    }

    private List<RecipeIngredientEntity> buildRecipeIngredients(
        Long recipeId,
        List<RecipeIngredientRequest> requests
    ) {
        validateIngredientRequests(requests);
        return requests.stream()
            .map(request -> {
                RecipeIngredientEntity row = new RecipeIngredientEntity();
                row.setRecipeId(recipeId);
                row.setIngredientId(request.ingredientId());
                row.setQuantity(request.quantity());
                row.setUnit(cleanRequired(request.unit(), "食材计量单位不能为空"));
                row.setCore(Boolean.TRUE.equals(request.core()));
                return row;
            })
            .toList();
    }

    private Map<Long, IngredientEntity> ingredientsById(List<RecipeIngredientRequest> requests) {
        validateIngredientRequests(requests);
        return ingredientsById(requests.stream()
            .map(RecipeIngredientRequest::ingredientId)
            .toList());
    }

    private Map<Long, IngredientEntity> ingredientsById(Collection<Long> ids) {
        List<Long> distinctIds = ids.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        List<IngredientEntity> ingredients = ingredientMapper.selectBatchIds(distinctIds);
        Map<Long, IngredientEntity> byId = ingredients == null
            ? Map.of()
            : ingredients.stream().collect(Collectors.toMap(
                IngredientEntity::getId,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
            ));
        if (byId.size() != distinctIds.size()) {
            throw new BusinessException("食材不存在");
        }
        return byId;
    }

    private void validateIngredientRequests(List<RecipeIngredientRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("菜谱至少需要一个食材");
        }
        boolean invalid = requests.stream()
            .anyMatch(request -> request == null
                || request.ingredientId() == null
                || request.quantity() == null
                || request.quantity().compareTo(BigDecimal.ZERO) <= 0);
        if (invalid) {
            throw new BusinessException("菜谱食材参数不正确");
        }
    }

    private boolean matchesKeyword(RecipeEntity recipe, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim();
        return contains(recipe.getName(), normalized) || contains(recipe.getDescription(), normalized);
    }

    private boolean matchesDietGoal(RecipeEntity recipe, DietGoal dietGoal) {
        return dietGoal == null || splitList(recipe.getTargetGoals()).contains(dietGoal.name());
    }

    private boolean matchesTag(RecipeEntity recipe, String tag) {
        if (tag == null || tag.isBlank()) {
            return true;
        }
        String normalized = tag.trim();
        return splitList(recipe.getTasteTags()).contains(normalized)
            || splitList(recipe.getHealthTags()).contains(normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private String toStepsJson(List<String> steps) {
        List<String> cleanSteps = cleanList(steps);
        if (cleanSteps.isEmpty()) {
            throw new BusinessException("菜谱步骤不能为空");
        }
        try {
            return objectMapper.writeValueAsString(cleanSteps);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("菜谱步骤格式不正确");
        }
    }

    private List<String> parseSteps(String steps) {
        if (steps == null || steps.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(steps, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            return splitList(steps);
        }
    }

    private List<String> enrichSteps(List<String> steps) {
        if (steps.isEmpty()) {
            return steps;
        }
        return java.util.stream.IntStream.range(0, steps.size())
            .mapToObj(index -> enrichStep(steps.get(index), index))
            .toList();
    }

    private String enrichStep(String step, int index) {
        if (step.length() >= 36) {
            return step;
        }
        return step + " " + switch (Math.min(index, 4)) {
            case 0 -> "先擦干表面水分，切配尽量保持大小一致；如果包含肉类、鱼虾或豆制品，可用少量盐、黑胡椒或生抽抓匀，静置5-10分钟更入味。";
            case 1 -> "保持小火到中火，先处理耐煮或需要煎香的食材，锅内只放少量油；看到边缘变色、香味出来或蔬菜颜色变亮后，再进入下一步。";
            case 2 -> "调味从少量开始，边翻拌边观察状态；加入蛋液、豆腐、米饭或面条时动作放轻，让食材均匀裹味但尽量保持完整。";
            case 3 -> "出锅前确认肉类完全熟透、蔬菜仍有脆感，汤汁或酱汁不要过多；装盘后再补葱花、柠檬汁或黑胡椒提香。";
            default -> "按顺序完成这一步，保持火候稳定，避免长时间大火导致食材出水或口感变老。";
        };
    }

    private String joinGoals(List<DietGoal> goals) {
        if (goals == null || goals.isEmpty()) {
            throw new BusinessException("目标类型不能为空");
        }
        return goals.stream()
            .filter(Objects::nonNull)
            .map(DietGoal::name)
            .distinct()
            .collect(Collectors.joining(","));
    }

    private String joinList(List<String> values) {
        return cleanList(values).stream().collect(Collectors.joining(","));
    }

    private List<String> cleanList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
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

    private String cleanRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
