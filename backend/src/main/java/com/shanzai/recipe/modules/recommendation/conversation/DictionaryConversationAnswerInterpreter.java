package com.shanzai.recipe.modules.recommendation.conversation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DictionaryConversationAnswerInterpreter implements ConversationAnswerInterpreter {
    private static final Pattern AMOUNT = Pattern.compile(
            "(?:\\d+(?:\\.\\d+)?|[一二三四五六七八九十两]+)\\s*(克|g|千克|kg|毫升|ml|个)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TIME = Pattern.compile(
            "(?:\\d+(?:\\.\\d+)?|[一二三四五六七八九十两]+)\\s*(小时|时|分钟|分|min|h)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SERVINGS = Pattern.compile(
            "(?:\\d+(?:\\.\\d+)?|[一二三四五六七八九十两]+)\\s*(个人|人|人份|份)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MIXED_HOUR = Pattern.compile(
            "(?:(\\d+|[一二三四五六七八九十两]+)\\s*(?:个)?\\s*半小时|(\\d+|[一二三四五六七八九十两]+)\\s*小时\\s*半)"
    );
    private static final Pattern NO_RESTRICTION = Pattern.compile(
            "无忌口|没有忌口|不忌口|无过敏|没有过敏|不过敏|都可以吃|都能吃|不挑食|没有饮食禁忌|没有禁忌"
    );
    private static final Pattern AI_EXPLICIT_NO_RESTRICTION = Pattern.compile(
            "无忌口|没有忌口|不忌口|无过敏|没有过敏|不过敏|没有(?:任何)?(?:饮食)?限制|无(?:任何)?(?:饮食)?限制"
    );
    private static final Pattern RESTRICTION_MARKER = Pattern.compile("不吃|忌口|忌|过敏|不能吃|不要吃");
    private static final Pattern EXCLUSION_MARKER = Pattern.compile("不吃|忌口|忌|不能吃|不要吃");
    private static final Pattern ALLERGY_SUFFIX = Pattern.compile("([^，、。；;！？!?]{1,8})(过敏|过敏原)");
    private static final Pattern RESTRICTION_PREFERENCE = Pattern.compile(
            "低盐|少盐|低糖|少糖|低脂|少油|不辣|清淡(?:饮食)?"
    );
    private static final List<String> SEPARATORS = List.of("，", "、", "。", "；", ";", "和", "及", "以及");
    private static final Map<String, String> FOOD_ALIASES = aliases();
    private static final Set<String> AI_NAME_PLACEHOLDERS = Set.of(
            "abc", "foobar", "unknown", "none", "测试", "测试东西", "测试食材", "未知", "未知食材", "随便"
    );
    private static final List<String> FOOD_TERMS = FOOD_ALIASES.keySet().stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    @Override
    public ConversationAnswerAnalysis interpret(
            ConversationStage stage,
            String content,
            RecommendationConversationContext context
    ) {
        String text = content == null ? "" : content.trim();
        if (!hasAlphaNumericOrChinese(text)) {
            return ConversationAnswerAnalysis.invalid();
        }

        RecommendationConversationContext current = context == null
                ? RecommendationConversationContext.empty()
                : context;
        List<IngredientOccurrence> occurrences = findIngredients(text);
        List<AvailableIngredientInput> ingredients = new ArrayList<>();
        List<String> newConflicts = new ArrayList<>();
        int previousIngredientEnd = 0;
        for (IngredientOccurrence occurrence : occurrences) {
            Amount amount = amountBefore(text, occurrence.start(), previousIngredientEnd);
            previousIngredientEnd = occurrence.end();
            if (amount == null) {
                ingredients.add(new AvailableIngredientInput(occurrence.name(), null, null, false));
            } else if (amount.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                newConflicts.add(occurrence.name() + "数量无效");
            } else {
                ingredients.add(new AvailableIngredientInput(
                        occurrence.name(), amount.quantity(), amount.unit(), true));
            }
        }

        List<String> unknown = stage == ConversationStage.INGREDIENTS
                ? new ArrayList<>(findVagueTerms(text, occurrences)) : new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        List<String> allergies = new ArrayList<>();
        RestrictionTerms specificRestrictions = findSpecificRestrictions(text);
        addUniqueAll(excluded, specificRestrictions.excluded());
        addUniqueAll(allergies, specificRestrictions.allergies());
        for (IngredientOccurrence occurrence : occurrences) {
            String clause = clauseAround(text, occurrence.start(), occurrence.end());
            String classifiedClause = NO_RESTRICTION.matcher(clause).replaceAll(" ");
            if (!RESTRICTION_MARKER.matcher(classifiedClause).find()) {
                continue;
            }
            if (classifiedClause.contains("过敏")) {
                addUnique(allergies, occurrence.name());
            } else {
                addUnique(excluded, occurrence.name());
            }
        }

        boolean explicitNoRestriction = NO_RESTRICTION.matcher(text).find();
        boolean restrictionsAnswered = explicitNoRestriction
                || specificRestrictions.preference()
                || !excluded.isEmpty() || !allergies.isEmpty();
        boolean clearRestrictions = explicitNoRestriction && excluded.isEmpty() && allergies.isEmpty();
        removeRestrictedIngredients(ingredients, excluded, allergies);
        Integer cookingTime = extractTime(text);
        Integer servings = extractServings(text);
        String intentText = stage == ConversationStage.INTENT ? text : null;
        String dietGoal = extractDietGoal(text);
        if (!hasMeaningfulSignal(stage, text, occurrences, unknown, cookingTime, servings,
                restrictionsAnswered, dietGoal)) {
            return ConversationAnswerAnalysis.invalid();
        }
        List<String> remainingUnknown = unresolved(current.unknownTerms(), unknown, text, ingredients);
        List<String> remainingConflicts = unresolvedConflicts(
                current.conflicts(), newConflicts, text, ingredients);
        BigDecimal confidence = ingredients.isEmpty() && cookingTime == null && servings == null
                && !restrictionsAnswered && intentText == null
                ? new BigDecimal("0.60") : new BigDecimal("0.90");

        return new ConversationAnswerAnalysis(
                true, intentText, dietGoal, ingredients, excluded, allergies,
                cookingTime, servings, remainingUnknown, remainingConflicts,
                restrictionsAnswered, clearRestrictions, confidence
        );
    }

    ConversationAnswerAnalysis normalizeAiAnalysis(
            ConversationAnswerAnalysis candidate,
            ConversationStage stage,
            String content,
            RecommendationConversationContext context
    ) {
        ConversationAnswerAnalysis local = interpret(stage, content, context);
        boolean localRelevant = local.relevant();
        if (!localRelevant && !hasAlphaNumericOrChinese(content == null ? "" : content.trim())) {
            return local;
        }

        List<AvailableIngredientInput> ingredients = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        List<String> localConflicts = localRelevant ? local.conflicts() : List.of();
        for (AvailableIngredientInput ingredient : candidate.availableIngredients()) {
            if (blockedByLocalConflict(localConflicts, ingredient)) {
                continue;
            }
            addValidatedIngredient(ingredients, conflicts, ingredient);
        }
        for (AvailableIngredientInput ingredient : localRelevant
                ? local.availableIngredients() : List.<AvailableIngredientInput>of()) {
            addValidatedIngredient(ingredients, conflicts, ingredient);
        }

        List<String> excluded = normalizedAiRestrictionNames(candidate.excludedIngredients(), conflicts);
        addUniqueAll(excluded, localRelevant ? local.excludedIngredients() : List.of());
        List<String> allergies = normalizedAiRestrictionNames(candidate.allergyIngredients(), conflicts);
        addUniqueAll(allergies, localRelevant ? local.allergyIngredients() : List.of());
        removeRestrictedIngredients(ingredients, excluded, allergies);
        boolean aiExplicitNoRestriction = candidate.relevant()
                && candidate.restrictionsAnswered()
                && excluded.isEmpty()
                && allergies.isEmpty()
                && AI_EXPLICIT_NO_RESTRICTION.matcher(content == null ? "" : content).find();
        boolean restrictionsAnswered = (localRelevant && local.restrictionsAnswered())
                || (candidate.restrictionsAnswered() && (!excluded.isEmpty() || !allergies.isEmpty()))
                || aiExplicitNoRestriction;
        boolean clearRestrictions = localRelevant && local.clearRestrictions() || aiExplicitNoRestriction;

        List<String> unknown = new ArrayList<>(candidate.unknownTerms());
        addUniqueAll(unknown, localRelevant ? local.unknownTerms() : List.of());
        List<String> candidateConflicts = new ArrayList<>(candidate.conflicts());
        addUniqueAll(candidateConflicts, conflicts);
        unknown = unresolved(context == null ? List.of() : context.unknownTerms(), unknown, content, ingredients);
        List<String> aiConflicts = unresolvedConflicts(
                context == null ? List.of() : context.conflicts(),
                candidateConflicts,
                content,
                ingredients
        );
        conflicts = new ArrayList<>(localConflicts);
        addUniqueAll(conflicts, aiConflicts);

        Integer time = candidate.cookingTime() != null && candidate.cookingTime() > 0
                ? candidate.cookingTime() : localRelevant ? local.cookingTime() : null;
        Integer people = candidate.servings() != null && candidate.servings() >= 1
                ? candidate.servings() : localRelevant ? local.servings() : null;
        boolean aiHasValidSignal = hasText(candidate.intentText())
                || hasText(candidate.dietGoal())
                || !ingredients.isEmpty()
                || !excluded.isEmpty()
                || !allergies.isEmpty()
                || time != null
                || people != null
                || !unknown.isEmpty()
                || restrictionsAnswered;
        return new ConversationAnswerAnalysis(
                localRelevant || candidate.relevant() && aiHasValidSignal,
                hasText(candidate.intentText()) ? candidate.intentText() : localRelevant ? local.intentText() : null,
                hasText(candidate.dietGoal()) ? candidate.dietGoal() : localRelevant ? local.dietGoal() : null,
                ingredients, excluded, allergies, time, people, unknown, conflicts,
                restrictionsAnswered, clearRestrictions, candidate.confidence()
        );
    }

    private void addValidatedIngredient(
            List<AvailableIngredientInput> ingredients,
            List<String> conflicts,
            AvailableIngredientInput input
    ) {
        if (input == null || input.name() == null || input.name().trim().isEmpty()) {
            addUnique(conflicts, "未命名食材");
            return;
        }
        String name = normalizeName(input.name());
        if (!hasMeaningfulAiIngredientName(name)) {
            addUnique(conflicts, "食材名称无效");
            return;
        }
        boolean hasQuantityFields = input.quantity() != null
                || input.unit() != null && !input.unit().isBlank();
        if (input.quantityKnown()
                && (input.quantity() == null || input.quantity().compareTo(BigDecimal.ZERO) <= 0
                || !isSupportedUnit(input.unit()))) {
            addUnique(conflicts, name + "数量无效");
            return;
        }
        if (!input.quantityKnown() && hasQuantityFields) {
            addUnique(conflicts, name + "数量无效");
            return;
        }
        AvailableIngredientInput normalized = new AvailableIngredientInput(
                name,
                input.quantity(),
                input.quantityKnown() ? normalizeUnit(input.unit()) : null,
                input.quantityKnown()
        );
        for (int index = 0; index < ingredients.size(); index++) {
            AvailableIngredientInput existing = ingredients.get(index);
            if (!existing.name().equals(name)) {
                continue;
            }
            if (!existing.quantityKnown() && normalized.quantityKnown()) {
                ingredients.set(index, normalized);
            }
            return;
        }
        ingredients.add(normalized);
    }

    private void removeRestrictedIngredients(
            List<AvailableIngredientInput> ingredients,
            List<String> excluded,
            List<String> allergies
    ) {
        ingredients.removeIf(ingredient -> excluded.contains(ingredient.name())
                || allergies.contains(ingredient.name()));
    }

    private boolean blockedByLocalConflict(
            List<String> conflicts,
            AvailableIngredientInput ingredient
    ) {
        if (ingredient == null || ingredient.name() == null || ingredient.name().isBlank()) {
            return conflicts.contains("未命名食材");
        }
        String name = normalizeName(ingredient.name());
        return conflicts.stream().anyMatch(value ->
                value.equals(name + "数量无效") || value.startsWith(name + "数量"));
    }

    private List<IngredientOccurrence> findIngredients(String text) {
        List<IngredientOccurrence> found = new ArrayList<>();
        for (String term : FOOD_TERMS) {
            Matcher matcher = Pattern.compile(Pattern.quote(term)).matcher(text);
            while (matcher.find()) {
                IngredientOccurrence occurrence = new IngredientOccurrence(
                        normalizeName(term), matcher.start(), matcher.end());
                boolean overlaps = found.stream().anyMatch(existing ->
                        existing.start() < occurrence.end() && occurrence.start() < existing.end());
                if (!overlaps) {
                    found.add(occurrence);
                }
            }
        }
        found.sort(Comparator.comparingInt(IngredientOccurrence::start));
        return found;
    }

    private List<String> findVagueTerms(String text, List<IngredientOccurrence> occurrences) {
        StringBuilder masked = new StringBuilder(text);
        for (IngredientOccurrence occurrence : occurrences) {
            for (int index = occurrence.start(); index < occurrence.end(); index++) {
                masked.setCharAt(index, ' ');
            }
        }
        List<String> result = new ArrayList<>();
        if (masked.indexOf("肉") >= 0) {
            result.add("肉");
        }
        if (masked.indexOf("菜") >= 0) {
            result.add("菜");
        }
        return result;
    }

    private Amount amountBefore(String text, int ingredientStart, int lowerBound) {
        int start = ingredientStart;
        while (start > lowerBound && !isSeparatorBefore(text, start - 1)) {
            start--;
        }
        Matcher matcher = AMOUNT.matcher(text.substring(Math.max(start, lowerBound), ingredientStart).trim());
        if (!matcher.find()) {
            return null;
        }
        String amount = matcher.group();
        String unit = matcher.group(1);
        while (matcher.find()) {
            amount = matcher.group();
            unit = matcher.group(1);
        }
        String numberText = amount.replaceAll("\\s*(克|g|千克|kg|毫升|ml|个)\\s*$", "").trim();
        return new Amount(parseNumber(numberText), normalizeUnit(unit));
    }

    private boolean isSeparatorBefore(String text, int index) {
        for (String separator : SEPARATORS) {
            int start = index - separator.length() + 1;
            if (start >= 0 && text.startsWith(separator, start)) {
                return true;
            }
        }
        return false;
    }

    private Integer extractTime(String text) {
        Matcher mixed = MIXED_HOUR.matcher(text);
        if (mixed.find()) {
            String number = mixed.group(1) == null ? mixed.group(2) : mixed.group(1);
            return parseNumber(number).multiply(BigDecimal.valueOf(60)).add(BigDecimal.valueOf(30)).intValue();
        }
        if (text.contains("半小时") || text.contains("半时")) {
            return 30;
        }
        Matcher matcher = TIME.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        BigDecimal value = parseNumber(matcher.group().replaceAll("\\s*(小时|时|分钟|分|min|h)\\s*$", ""));
        String unit = matcher.group(1).toLowerCase(Locale.ROOT);
        if (unit.equals("小时") || unit.equals("时") || unit.equals("h")) {
            value = value.multiply(BigDecimal.valueOf(60));
        }
        return value.setScale(0, RoundingMode.DOWN).intValue();
    }

    private Integer extractServings(String text) {
        Matcher matcher = SERVINGS.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return parseNumber(matcher.group().replaceAll("\\s*(个人|人|人份|份)\\s*$", ""))
                .setScale(0, RoundingMode.DOWN).intValue();
    }

    private String extractDietGoal(String text) {
        if (text.contains("减脂") || text.contains("减肥")) {
            return "FAT_LOSS";
        }
        if (text.contains("增肌")) {
            return "MUSCLE_GAIN";
        }
        if (text.contains("低糖") || text.contains("控糖")) {
            return "LOW_SUGAR";
        }
        return null;
    }

    private RestrictionTerms findSpecificRestrictions(String text) {
        String searchable = NO_RESTRICTION.matcher(text).replaceAll(" ");
        List<String> excluded = new ArrayList<>();
        List<String> allergies = new ArrayList<>();
        Matcher exclusion = EXCLUSION_MARKER.matcher(searchable);
        while (exclusion.find()) {
            int end = restrictionBoundary(searchable, exclusion.end());
            String phrase = searchable.substring(exclusion.end(), end);
            for (String term : phrase.split("、|和|及|以及|\\s+")) {
                addRestrictionTerm(excluded, term);
            }
        }

        Matcher allergy = ALLERGY_SUFFIX.matcher(searchable);
        while (allergy.find()) {
            addRestrictionTerm(allergies, allergy.group(1));
        }
        return new RestrictionTerms(
                excluded,
                allergies,
                RESTRICTION_PREFERENCE.matcher(searchable).find()
        );
    }

    private int restrictionBoundary(String text, int start) {
        int end = text.length();
        for (int index = start; index < text.length(); index++) {
            char character = text.charAt(index);
            if ("，。；;！？!?".indexOf(character) >= 0) {
                end = index;
                break;
            }
        }
        return Math.min(end, start + 8);
    }

    private void addRestrictionTerm(List<String> target, String value) {
        String term = value == null ? "" : value.trim().replaceAll("\\s+", "");
        term = term.replaceFirst("^(?:(?:但是|而且|并且|同时|关于|对|但|我|用户|有|是)+)", "");
        term = term.replaceAll("(食物|食品|之类|等|吧|呀|呢)$", "");
        if (term.isEmpty() || !hasAlphaNumericOrChinese(term)
                || NO_RESTRICTION.matcher(term).find()) {
            return;
        }
        addUnique(target, normalizeName(term));
    }

    private boolean hasMeaningfulSignal(
            ConversationStage stage,
            String text,
            List<IngredientOccurrence> occurrences,
            List<String> unknown,
            Integer cookingTime,
            Integer servings,
            boolean restrictionsAnswered,
            String dietGoal
    ) {
        if (!occurrences.isEmpty() || !unknown.isEmpty() || cookingTime != null || servings != null
                || restrictionsAnswered || dietGoal != null) {
            return true;
        }
        return stage == ConversationStage.INTENT
                && text.matches(".*(吃|菜|餐|饮食|推荐|食谱|做|口味|想要|想吃|清淡|健康).*" );
    }

    private String clauseAround(String text, int start, int end) {
        int left = start;
        while (left > 0 && "，、。；;".indexOf(text.charAt(left - 1)) < 0) {
            left--;
        }
        int right = end;
        while (right < text.length() && "，、。；;".indexOf(text.charAt(right)) < 0) {
            right++;
        }
        return text.substring(left, right);
    }

    private List<String> unresolved(
            List<String> previous,
            List<String> current,
            String content,
            List<AvailableIngredientInput> ingredients
    ) {
        List<String> result = new ArrayList<>();
        addUnresolved(result, previous, content, ingredients, false);
        addUnresolved(result, current, content, ingredients, false);
        return deduplicate(result);
    }

    private List<String> unresolvedConflicts(
            List<String> previous,
            List<String> current,
            String content,
            List<AvailableIngredientInput> ingredients
    ) {
        List<String> result = new ArrayList<>();
        addUnresolved(result, previous, content, ingredients, true);
        addUnresolved(result, current, content, ingredients, true);
        return deduplicate(result);
    }

    private void addUnresolved(
            List<String> target,
            List<String> values,
            String content,
            List<AvailableIngredientInput> ingredients,
            boolean conflict
    ) {
        for (String value : values == null ? List.<String>of() : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            boolean resolved = content.contains(value)
                    && (!ingredients.isEmpty() || conflict && content.matches(".*\\d+.*"));
            if (!resolved && conflict && ingredients.stream().anyMatch(input ->
                    input.quantityKnown() && value.startsWith(input.name() + "数量"))) {
                resolved = true;
            }
            if (!resolved) {
                addUnique(target, value.trim());
            }
        }
    }

    private List<String> normalizedAiRestrictionNames(List<String> values, List<String> conflicts) {
        List<String> result = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    addUnique(conflicts, "食材名称无效");
                    continue;
                }
                String name = normalizeName(value);
                if (!hasMeaningfulAiIngredientName(name)) {
                    addUnique(conflicts, "食材名称无效");
                    continue;
                }
                addUnique(result, name);
            }
        }
        return result;
    }

    private String normalizeName(String value) {
        String trimmed = value == null ? "" : value.trim().replaceAll("\\s+", "");
        return FOOD_ALIASES.getOrDefault(trimmed, trimmed);
    }

    private String normalizeUnit(String unit) {
        return switch (canonicalUnit(unit)) {
            case "克", "g" -> "g";
            case "千克", "kg" -> "kg";
            case "毫升", "ml" -> "ml";
            case "个" -> "个";
            default -> "";
        };
    }

    private boolean isSupportedUnit(String unit) {
        return switch (canonicalUnit(unit)) {
            case "克", "g", "千克", "kg", "毫升", "ml", "个" -> true;
            default -> false;
        };
    }

    private String canonicalUnit(String unit) {
        return unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
    }

    private BigDecimal parseNumber(String text) {
        if (text.matches("\\d+(?:\\.\\d+)?")) {
            return new BigDecimal(text);
        }
        Map<Character, Integer> digits = Map.ofEntries(
                Map.entry('一', 1), Map.entry('二', 2), Map.entry('两', 2), Map.entry('三', 3),
                Map.entry('四', 4), Map.entry('五', 5), Map.entry('六', 6), Map.entry('七', 7),
                Map.entry('八', 8), Map.entry('九', 9)
        );
        if (text.equals("十")) {
            return BigDecimal.TEN;
        }
        int total = 0;
        int current = 0;
        for (char character : text.toCharArray()) {
            if (digits.containsKey(character)) {
                current = digits.get(character);
            } else if (character == '十') {
                total += (current == 0 ? 1 : current) * 10;
                current = 0;
            }
        }
        return BigDecimal.valueOf(total + current);
    }

    private static boolean hasAlphaNumericOrChinese(String value) {
        return value.codePoints().anyMatch(codePoint ->
                Character.isLetterOrDigit(codePoint) || Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private static boolean hasLetterOrChinese(String value) {
        return value.codePoints().anyMatch(Character::isLetter);
    }

    private boolean hasMeaningfulAiIngredientName(String value) {
        String normalized = value.trim().replaceAll("\\s+", "");
        String lowerCase = normalized.toLowerCase(Locale.ROOT);
        return hasLetterOrChinese(normalized)
                && !AI_NAME_PLACEHOLDERS.contains(lowerCase)
                && !lowerCase.matches("(?:test|testing|placeholder|unknown|none|foo|bar)[0-9_-]*");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void addUnique(List<String> target, String value) {
        if (value != null && !value.isBlank() && !target.contains(value)) {
            target.add(value);
        }
    }

    private static void addUniqueAll(List<String> target, List<String> values) {
        if (values != null) {
            values.forEach(value -> addUnique(target, value));
        }
    }

    private static List<String> deduplicate(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private static Map<String, String> aliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("西蓝花", "西兰花");
        aliases.put("西兰花", "西兰花");
        aliases.put("番茄", "番茄");
        aliases.put("鸡胸", "鸡胸肉");
        aliases.put("鸡胸肉", "鸡胸肉");
        aliases.put("鸡蛋", "鸡蛋");
        aliases.put("猪肉", "猪肉");
        aliases.put("牛肉", "牛肉");
        aliases.put("鸡肉", "鸡肉");
        aliases.put("鱼", "鱼");
        aliases.put("虾", "虾");
        aliases.put("香菜", "香菜");
        aliases.put("花生", "花生");
        aliases.put("芹菜", "芹菜");
        aliases.put("洋葱", "洋葱");
        aliases.put("土豆", "土豆");
        aliases.put("胡萝卜", "胡萝卜");
        aliases.put("白菜", "白菜");
        aliases.put("菠菜", "菠菜");
        aliases.put("豆腐", "豆腐");
        aliases.put("米饭", "米饭");
        aliases.put("面条", "面条");
        aliases.put("牛奶", "牛奶");
        return aliases;
    }

    private record IngredientOccurrence(String name, int start, int end) {
    }

    private record Amount(BigDecimal quantity, String unit) {
    }

    private record RestrictionTerms(
            List<String> excluded,
            List<String> allergies,
            boolean preference
    ) {
    }
}
