package com.shanzai.recipe.modules.recommendation.conversation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HttpConversationAnswerInterpreter implements ConversationAnswerInterpreter {
    private static final Logger log = LoggerFactory.getLogger(HttpConversationAnswerInterpreter.class);
    private static final List<String> REQUIRED_FIELDS = List.of(
            "relevant", "intentText", "dietGoal", "availableIngredients",
            "excludedIngredients", "allergyIngredients", "cookingTime", "servings",
            "unknownTerms", "conflicts", "confidence", "restrictionsAnswered"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final DictionaryConversationAnswerInterpreter dictionary;

    public HttpConversationAnswerInterpreter(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${app.deepseek.api-key:}") String apiKey,
            @Value("${app.deepseek.model:deepseek-v4-flash}") String model,
            @Value("${app.deepseek.connect-timeout:2s}") Duration connectTimeout,
            @Value("${app.deepseek.read-timeout:8s}") Duration readTimeout
    ) {
        this(restClientBuilder, objectMapper, baseUrl, apiKey, model, connectTimeout, readTimeout,
                new DictionaryConversationAnswerInterpreter());
    }

    HttpConversationAnswerInterpreter(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            String baseUrl,
            String apiKey,
            String model,
            Duration connectTimeout,
            Duration readTimeout,
            DictionaryConversationAnswerInterpreter dictionary
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(connectTimeout, readTimeout))
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.dictionary = dictionary;
    }

    HttpConversationAnswerInterpreter(
            RestClient restClient,
            ObjectMapper objectMapper,
            String apiKey,
            String model,
            DictionaryConversationAnswerInterpreter dictionary
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.dictionary = dictionary;
    }

    HttpConversationAnswerInterpreter(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            String baseUrl,
            String apiKey,
            String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.dictionary = new DictionaryConversationAnswerInterpreter();
    }

    @Override
    public ConversationAnswerAnalysis interpret(
            ConversationStage stage,
            String content,
            RecommendationConversationContext context
    ) {
        RecommendationConversationContext current = context == null
                ? RecommendationConversationContext.empty() : context;
        if (apiKey.isBlank()) {
            log.info("[DEEPSEEK_CONVERSATION_FALLBACK_NO_KEY] 已使用本地对话解释器");
            return dictionary.interpret(stage, content, current);
        }

        try {
            Optional<ConversationAnswerAnalysis> parsed = request(stage, content, current);
            if (parsed.isEmpty()) {
                log.warn("[DEEPSEEK_CONVERSATION_FALLBACK_INVALID_RESPONSE] 已使用本地对话解释器");
                return dictionary.interpret(stage, content, current);
            }
            return dictionary.normalizeAiAnalysis(parsed.get(), stage, content, current);
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("[DEEPSEEK_CONVERSATION_FALLBACK_ERROR] 已使用本地对话解释器：{}",
                    exception.getClass().getSimpleName());
            return dictionary.interpret(stage, content, current);
        }
    }

    private Optional<ConversationAnswerAnalysis> request(
            ConversationStage stage,
            String content,
            RecommendationConversationContext context
    ) throws JsonProcessingException {
        ConversationRequest request = new ConversationRequest(
                model,
                List.of(
                        new ConversationMessage("system", systemPrompt()),
                        new ConversationMessage("user", userPrompt(stage, content, context))
                ),
                Map.of("type", "json_object"),
                0.2
        );
        String response = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
        if (response == null || response.isBlank()) {
            return Optional.empty();
        }
        JsonNode root = objectMapper.readTree(response);
        JsonNode choices = root == null ? null : root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()
                || choices.get(0) == null || !choices.get(0).has("message")) {
            return Optional.empty();
        }
        JsonNode message = choices.get(0).get("message");
        JsonNode contentNode = message == null ? null : message.get("content");
        if (contentNode == null || !contentNode.isTextual() || contentNode.asText().isBlank()) {
            return Optional.empty();
        }
        JsonNode answer = objectMapper.readTree(contentNode.asText());
        return parseAnswer(answer);
    }

    private Optional<ConversationAnswerAnalysis> parseAnswer(JsonNode answer) {
        if (answer == null || !answer.isObject()
                || REQUIRED_FIELDS.stream().anyMatch(field -> !answer.has(field))) {
            return Optional.empty();
        }
        if (!answer.get("relevant").isBoolean() || !answer.get("restrictionsAnswered").isBoolean()
                || !answer.get("availableIngredients").isArray()
                || !answer.get("excludedIngredients").isArray()
                || !answer.get("allergyIngredients").isArray()
                || !answer.get("unknownTerms").isArray()
                || !answer.get("conflicts").isArray()
                || !answer.get("confidence").isNumber()) {
            return Optional.empty();
        }
        if (!nullableText(answer.get("intentText")) || !nullableText(answer.get("dietGoal"))
                || !nullableInteger(answer.get("cookingTime")) || !nullableInteger(answer.get("servings"))) {
            return Optional.empty();
        }

        List<AvailableIngredientInput> ingredients = new ArrayList<>();
        for (JsonNode node : answer.get("availableIngredients")) {
            if (node == null || !node.isObject()
                    || !node.has("name") || !node.has("quantity")
                    || !node.has("unit") || !node.has("quantityKnown")
                    || !nullableText(node.get("name")) || !nullableDecimal(node.get("quantity"))
                    || !nullableText(node.get("unit")) || !node.get("quantityKnown").isBoolean()) {
                return Optional.empty();
            }
            ingredients.add(new AvailableIngredientInput(
                    node.get("name").isNull() ? null : node.get("name").asText(),
                    node.get("quantity").isNull() ? null : new BigDecimal(node.get("quantity").asText()),
                    node.get("unit").isNull() ? null : node.get("unit").asText(),
                    node.get("quantityKnown").asBoolean()
            ));
        }
        List<String> excluded = parseStrings(answer.get("excludedIngredients"));
        List<String> allergies = parseStrings(answer.get("allergyIngredients"));
        List<String> unknown = parseStrings(answer.get("unknownTerms"));
        List<String> conflicts = parseStrings(answer.get("conflicts"));
        if (excluded == null || allergies == null || unknown == null || conflicts == null) {
            return Optional.empty();
        }
        return Optional.of(new ConversationAnswerAnalysis(
                answer.get("relevant").asBoolean(),
                nullableTextValue(answer.get("intentText")),
                nullableTextValue(answer.get("dietGoal")),
                ingredients,
                excluded,
                allergies,
                nullableIntegerValue(answer.get("cookingTime")),
                nullableIntegerValue(answer.get("servings")),
                unknown,
                conflicts,
                new BigDecimal(answer.get("confidence").asText()),
                answer.get("restrictionsAnswered").asBoolean()
        ));
    }

    private List<String> parseStrings(JsonNode node) {
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            if (value == null || !value.isTextual()) {
                return null;
            }
            values.add(value.asText());
        }
        return values;
    }

    private boolean nullableText(JsonNode node) {
        return node != null && (node.isNull() || node.isTextual());
    }

    private boolean nullableDecimal(JsonNode node) {
        return node != null && (node.isNull() || node.isNumber());
    }

    private boolean nullableInteger(JsonNode node) {
        return node != null && (node.isNull() || node.isIntegralNumber());
    }

    private String nullableTextValue(JsonNode node) {
        return node.isNull() ? null : node.asText();
    }

    private Integer nullableIntegerValue(JsonNode node) {
        return node.isNull() ? null : node.asInt();
    }

    private SimpleClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    private String systemPrompt() {
        return """
                你是膳哉的自然语言字段提取器。
                只提取字段，不生成菜谱，不访问数据库，不覆盖当前 context 中已经确认的字段。
                必须只返回 JSON，不要 Markdown 或额外说明。
                JSON 字段固定为 relevant、intentText、dietGoal、availableIngredients、excludedIngredients、
                allergyIngredients、cookingTime、servings、unknownTerms、conflicts、confidence、restrictionsAnswered。
                availableIngredients 的每项固定为 name、quantity、unit、quantityKnown。
                restrictionsAnswered 只有用户明确回答限制条件，或明确给出忌口/过敏食材时才为 true。
                """;
    }

    private String userPrompt(
            ConversationStage stage,
            String content,
            RecommendationConversationContext context
    ) throws JsonProcessingException {
        return """
                当前 stage：%s
                当前 context JSON：%s
                用户回答：%s

                请只提取用户回答中明确出现的字段。保持已有确认字段，不要推测缺失值。
                输出必须是 JSON object，字段和类型必须严格符合系统提示。
                """.formatted(stage, objectMapper.writeValueAsString(context), content == null ? "" : content);
    }

    private record ConversationRequest(
            String model,
            List<ConversationMessage> messages,
            Map<String, String> response_format,
            double temperature
    ) {
    }

    private record ConversationMessage(String role, String content) {
    }
}
