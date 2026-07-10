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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DictionaryConversationAnswerInterpreter implements ConversationAnswerInterpreter {
    private static final Pattern NUMBER = Pattern.compile("(?:\\d+(?:\\.\\d+)?|[零一二三四五六七八九十百千万两]+)");
    private static final Pattern AMOUNT = Pattern.compile(
            "(?:\\d+(?:\\.\\d+)?|[零一二三四五六七八九十百千万两]+)\\s*(克|g|千克|kg|毫升|ml|个)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TIME = Pattern.compile(
            "(?:\\d+(?:\\.\\d+)?|[一二三四五六七八九十百千万两]+)\\s*(小时|时|分钟|分|min|h)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SERVINGS = Pattern.compile(
            "(?:\\d+(?:\\.\\d+)?|[一二三四五六七八九十百千万两]+)\\s*(个人|人|人份|份)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NO_RESTRICTION = Pattern.compile(
            "无忌口|没有忌口|无过敏|没有过敏|不过敏|都可以吃|都能吃|不挑食|没有饮食禁忌|没有禁忌"
    );
    private static final Pattern RESTRICTION_MARKER = Pattern.compile("不吃|忌口|忌|过敏|不能吃|不要吃");
    private static final Pattern EXCLUSION_MARKER = Pattern.compile("不吃|忌口|忌|不能吃|不要吃");
    private static final Pattern ALLERGY_SUFFIX = Pattern.compile("([^，、。；;！？!?]{1,8})(过敏|过敏原)");
    private static final Pattern RESTRICTION_PREFERENCE = Pattern.compile(
            "低盐|少盐|低糖|少糖|低脂|少油|不辣|清淡(?:饮食)?"
    );
    private static final List<String> SEPARATORS = List.of("，", "、", "。", "；", ";", "和", "及", "以及");
    private static final Map<String, String> FOOD_ALIASES = aliases();
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
        for (IngredientOccurrence occurrence : occurrences) {
            Amount amount = amountBefore(text, occurrence.start());
            if (amount == null) {
                ingredients.add(new AvailableIngredientInput(occurrence.name(), null, null, false));
            } else if (amount.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                newConflicts.add(occurrence.name() + "数量无效");
            } else {
                ingredients.add(new AvailableIngredientInput(
                        occurrence.name(), amount.quantity(), amount.unit(), true));
            }
        }

        List<String> unknown = new ArrayList<>(findVagueTerms(text, occurrences));
        List<String> excluded = new ArrayList<>();
        List<String> allergies = new ArrayList<>();
        RestrictionTerms specificRestrictions = findSpecificRestrictions(text);
        addUniqueAll(excluded, specificRestrictions.excluded());
        addUniqueAll(allergies, specificRestrictions.allergies());
        for (IngredientOccurrence occurrence : occurrences) {
            String clause = clauseAround(text, occurrence.start(), occurrence.end());
            if (!RESTRICTION_MARKER.matcher(clause).find()) {
                continue;
            }
            if (clause.contains("过敏")) {
                addUnique(allergies, occurrence.name());
            } else {
                addUnique(excluded, occurrence.name());
            }
        }

        boolean restrictionsAnswered = NO_RESTRICTION.matcher(text).find()
                || specificRestrictions.preference()
                || !excluded.isEmpty() || !allergies.isEmpty();
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
                confidence, restrictionsAnswered
        );
    }

    ConversationAnswerAnalysis normalizeAiAnalysis(
            ConversationAnswerAnalysis candidate,
            ConversationStage stage,
            String content,
            RecommendationConversationContext context
    ) {
        ConversationAnswerAnalysis local = interpret(stage, content, context);
        if (!local.relevant()) {
            return local;
        }

        List<AvailableIngredientInput> ingredients = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        for (AvailableIngredientInput ingredient : candidate.availableIngredients()) {
            addValidatedIngredient(ingredients, conflicts, ingredient);
        }
        for (AvailableIngredientInput ingredient : local.availableIngredients()) {
            addValidatedIngredient(ingredients, conflicts, ingredient);
        }

        List<String> excluded = normalizedNames(candidate.excludedIngredients());
        addUniqueAll(excluded, local.excludedIngredients());
        List<String> allergies = normalizedNames(candidate.allergyIngredients());
        addUniqueAll(allergies, local.allergyIngredients());
        boolean restrictionsAnswered = local.restrictionsAnswered()
                || (candidate.restrictionsAnswered() && (!excluded.isEmpty() || !allergies.isEmpty()));

        List<String> unknown = new ArrayList<>(candidate.unknownTerms());
        addUniqueAll(unknown, local.unknownTerms());
        List<String> candidateConflicts = new ArrayList<>(candidate.conflicts());
        addUniqueAll(candidateConflicts, local.conflicts());
        addUniqueAll(candidateConflicts, conflicts);
        unknown = unresolved(context == null ? List.of() : context.unknownTerms(), unknown, content, ingredients);
        conflicts = unresolvedConflicts(
                context == null ? List.of() : context.conflicts(), candidateConflicts, content, ingredients);
        conflicts.addAll(unresolvedConflicts(List.of(), conflicts, content, ingredients));
        conflicts = deduplicate(conflicts);

        Integer time = candidate.cookingTime() != null && candidate.cookingTime() > 0
                ? candidate.cookingTime() : local.cookingTime();
        Integer people = candidate.servings() != null && candidate.servings() >= 1
                ? candidate.servings() : local.servings();
        return new ConversationAnswerAnalysis(
                candidate.relevant(),
                hasText(candidate.intentText()) ? candidate.intentText() : local.intentText(),
                hasText(candidate.dietGoal()) ? candidate.dietGoal() : local.dietGoal(),
                ingredients, excluded, allergies, time, people, unknown, conflicts,
                candidate.confidence(), restrictionsAnswered
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
        if (input.quantityKnown()
                && (input.quantity() == null || input.quantity().compareTo(BigDecimal.ZERO) <= 0
                || input.unit() == null || input.unit().isBlank())) {
            addUnique(conflicts, name + "数量无效");
            return;
        }
        AvailableIngredientInput normalized = new AvailableIngredientInput(
                name,
                input.quantity(),
                input.unit() == null ? null : input.unit().trim(),
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

    private Amount amountBefore(String text, int ingredientStart) {
        int start = ingredientStart;
        while (start > 0 && !isSeparatorBefore(text, start - 1)) {
            start--;
        }
        Matcher matcher = AMOUNT.matcher(text.substring(start, ingredientStart).trim());
        if (!matcher.find()) {
            return null;
        }
        String numberText = matcher.group().replaceAll("\\s*(克|g|千克|kg|毫升|ml|个)\\s*$", "").trim();
        BigDecimal quantity = parseNumber(numberText);
        return new Amount(quantity, normalizeUnit(matcher.group(1)));
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
            for (String term : phrase.split("和|及|以及|\\s+")) {
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
            if ("，、。；;！？!?".indexOf(character) >= 0) {
                end = index;
                break;
            }
        }
        return Math.min(end, start + 8);
    }

    private void addRestrictionTerm(List<String> target, String value) {
        String term = value == null ? "" : value.trim().replaceAll("\\s+", "");
        term = term.replaceFirst("^(对|关于)", "");
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
                    input.quantityKnown() && value.startsWith(input.name()))) {
                resolved = true;
            }
            if (!resolved) {
                addUnique(target, value.trim());
            }
        }
    }

    private List<String> normalizedNames(List<String> values) {
        List<String> result = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    addUnique(result, normalizeName(value));
                }
            }
        }
        return result;
    }

    private String normalizeName(String value) {
        String trimmed = value == null ? "" : value.trim().replaceAll("\\s+", "");
        return FOOD_ALIASES.getOrDefault(trimmed, trimmed);
    }

    private String normalizeUnit(String unit) {
        return switch (unit.toLowerCase(Locale.ROOT)) {
            case "克", "g" -> "g";
            case "千克", "kg" -> "kg";
            case "毫升", "ml" -> "ml";
            case "个" -> "个";
            default -> unit;
        };
    }

    private BigDecimal parseNumber(String text) {
        if (text.matches("\\d+(?:\\.\\d+)?")) {
            return new BigDecimal(text);
        }
        Map<Character, Integer> digits = Map.ofEntries(
                Map.entry('零', 0), Map.entry('一', 1), Map.entry('二', 2), Map.entry('两', 2),
                Map.entry('三', 3), Map.entry('四', 4), Map.entry('五', 5), Map.entry('六', 6),
                Map.entry('七', 7), Map.entry('八', 8), Map.entry('九', 9)
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
            } else if (character == '百') {
                total += (current == 0 ? 1 : current) * 100;
                current = 0;
            } else if (character == '千') {
                total += (current == 0 ? 1 : current) * 1000;
                current = 0;
            }
        }
        return BigDecimal.valueOf(total + current);
    }

    private static boolean hasAlphaNumericOrChinese(String value) {
        return value.codePoints().anyMatch(codePoint ->
                Character.isLetterOrDigit(codePoint) || Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
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
