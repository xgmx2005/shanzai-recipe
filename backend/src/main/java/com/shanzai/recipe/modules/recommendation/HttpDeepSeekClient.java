package com.shanzai.recipe.modules.recommendation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HttpDeepSeekClient implements DeepSeekClient {
    private static final Logger log = LoggerFactory.getLogger(HttpDeepSeekClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public HttpDeepSeekClient(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        @Value("${app.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
        @Value("${app.deepseek.api-key:}") String apiKey,
        @Value("${app.deepseek.model:deepseek-v4-flash}") String model,
        @Value("${app.deepseek.connect-timeout:2s}") Duration connectTimeout,
        @Value("${app.deepseek.read-timeout:8s}") Duration readTimeout
    ) {
        this.restClient = restClientBuilder
            .baseUrl(baseUrl)
            .requestFactory(requestFactory(connectTimeout, readTimeout))
            .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    private SimpleClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    @Override
    public Optional<AiRecommendationText> generateRecommendationText(AiRecommendationContext context) {
        if (apiKey.isBlank()) {
            log.info("[DEEPSEEK_FALLBACK_NO_KEY] DeepSeek API Key 未配置，已使用规则兜底");
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
                        new DeepSeekMessage("user", userPrompt(context))
                    ),
                    Map.of("type", "json_object"),
                    0.4
                ))
                .retrieve()
                .body(DeepSeekChatResponse.class);

            Optional<AiRecommendationText> parsed = parseResponse(response);
            if (parsed.isPresent()) {
                log.info("[DEEPSEEK_AI_SUCCESS] DeepSeek 推荐分析生成成功");
            } else {
                log.warn("[DEEPSEEK_FALLBACK_INVALID_RESPONSE] DeepSeek 返回内容不完整，已使用规则兜底");
            }
            return parsed;
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("[DEEPSEEK_FALLBACK_ERROR] DeepSeek 调用失败，已使用规则兜底：{}", exception.getMessage());
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
        if (isBlank(text.summary())
            || isBlank(text.healthTip())
            || isBlank(text.shoppingTip())
            || isBlank(text.topRecipeReason())) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String systemPrompt() {
        return """
            你是膳哉的知识库增强菜谱推荐助手。
            菜谱、营养、图片和购物清单食材已经由系统数据库确定。
            你只能解释推荐结果，不能编造新菜谱、图片、热量或购物清单食材。
            必须只返回 JSON，不要输出 Markdown 或额外说明。
            JSON 字段必须是 summary、healthTip、shoppingTip、topRecipeReason。
            """;
    }

    private String userPrompt(AiRecommendationContext context) throws JsonProcessingException {
        String recipesJson = objectMapper.writeValueAsString(context.recipes());
        String available = context.availableIngredients() == null || context.availableIngredients().isEmpty()
            ? "未填写"
            : String.join("、", context.availableIngredients());
        String excluded = context.excludedIngredients() == null || context.excludedIngredients().isEmpty()
            ? "无"
            : String.join("、", context.excludedIngredients());
        return """
            请基于以下系统已经评分完成的真实菜谱推荐结果，生成中文推荐分析。
            不要新增菜谱，不要新增购物食材，不要修改热量和蛋白质数据。

            饮食目标：%s
            已有食材：%s
            排除食材：%s
            烹饪时间：%s 分钟内
            推荐菜谱快照 JSON：%s

            输出 JSON 示例：
            {"summary":"本次推荐总结","healthTip":"健康提示","shoppingTip":"购物清单提示","topRecipeReason":"第一道菜的个性化推荐理由"}
            """.formatted(
                context.dietGoal(),
                available,
                excluded,
                context.cookingTime(),
                recipesJson
            );
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
