package com.shanzai.recipe.modules.recommendation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HttpDeepSeekClient implements DeepSeekClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public HttpDeepSeekClient(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        @Value("${app.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
        @Value("${app.deepseek.api-key:}") String apiKey,
        @Value("${app.deepseek.model:deepseek-v4-flash}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    @Override
    public Optional<AiRecommendationText> generateRecommendationText(
        String recipeName,
        String dietGoal,
        List<String> matchedIngredients
    ) {
        if (apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            DeepSeekChatResponse response = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeepSeekChatRequest(
                    model,
                    List.of(
                        new DeepSeekMessage("system", systemPrompt()),
                        new DeepSeekMessage("user", userPrompt(recipeName, dietGoal, matchedIngredients))
                    ),
                    Map.of("type", "json_object"),
                    0.4
                ))
                .retrieve()
                .body(DeepSeekChatResponse.class);

            return parseResponse(response);
        } catch (RuntimeException | JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private Optional<AiRecommendationText> parseResponse(DeepSeekChatResponse response) throws JsonProcessingException {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return Optional.empty();
        }
        String content = response.choices().get(0).message().content();
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        AiRecommendationText text = objectMapper.readValue(content, AiRecommendationText.class);
        if (text.reason() == null || text.reason().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    private String systemPrompt() {
        return """
            你是膳哉的健康菜谱推荐助手。必须只返回 JSON，不要输出 Markdown 或额外说明。
            JSON 字段必须是 reason、healthTip、shoppingTip。
            """;
    }

    private String userPrompt(String recipeName, String dietGoal, List<String> matchedIngredients) {
        String ingredients = matchedIngredients == null || matchedIngredients.isEmpty()
            ? "暂无明确匹配食材"
            : String.join("、", matchedIngredients);
        return """
            请为以下菜谱生成中文推荐文案：
            菜谱名称：%s
            饮食目标：%s
            已匹配食材：%s

            输出 JSON 示例：
            {"reason":"推荐理由","healthTip":"健康提示","shoppingTip":"购物清单提示"}
            """.formatted(recipeName, dietGoal, ingredients);
    }

    private record DeepSeekChatRequest(
        String model,
        List<DeepSeekMessage> messages,
        @JsonProperty("response_format") Map<String, String> responseFormat,
        double temperature
    ) {
    }

    private record DeepSeekMessage(String role, String content) {
    }

    private record DeepSeekChatResponse(List<DeepSeekChoice> choices) {
    }

    private record DeepSeekChoice(DeepSeekMessage message) {
    }
}
