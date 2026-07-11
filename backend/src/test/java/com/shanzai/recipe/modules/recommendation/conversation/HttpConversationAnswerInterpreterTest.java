package com.shanzai.recipe.modules.recommendation.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class HttpConversationAnswerInterpreterTest {
    @Test
    void sendsStageContextAndFixedJsonContractAndNormalizesAiResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://localhost/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("INGREDIENTS"),
                        org.hamcrest.Matchers.containsString("鸡胸肉"),
                        org.hamcrest.Matchers.containsString("availableIngredients"),
                        org.hamcrest.Matchers.containsString("restrictionsAnswered"),
                        org.hamcrest.Matchers.containsString("只提取字段"),
                        org.hamcrest.Matchers.containsString("不生成菜谱")
                )))
                .andRespond(withSuccess(aiResponse(), MediaType.APPLICATION_JSON));

        HttpConversationAnswerInterpreter interpreter = newInterpreter(builder, "test-key");
        ConversationAnswerAnalysis analysis = interpreter.interpret(
                ConversationStage.INGREDIENTS,
                "请识别鸡胸肉，没有忌口",
                contextWithChicken()
        );

        server.verify();
        assertTrue(analysis.relevant());
        assertEquals(List.of("鸡胸肉"),
                analysis.availableIngredients().stream().map(AvailableIngredientInput::name).toList());
        assertEquals(new BigDecimal("300"), analysis.availableIngredients().get(0).quantity());
        assertEquals("g", analysis.availableIngredients().get(0).unit());
        assertEquals(1, analysis.servings());
        assertTrue(analysis.restrictionsAnswered());
    }

    @Test
    void missingApiKeyFallsBackToDictionaryWithoutSendingRequest() {
        RestClient.Builder builder = RestClient.builder();
        HttpConversationAnswerInterpreter interpreter = newInterpreter(builder, "");

        ConversationAnswerAnalysis analysis = interpreter.interpret(
                ConversationStage.INGREDIENTS,
                "有两个鸡蛋，半小时内",
                RecommendationConversationContext.empty()
        );

        assertEquals(List.of("鸡蛋"),
                analysis.availableIngredients().stream().map(AvailableIngredientInput::name).toList());
        assertEquals(30, analysis.cookingTime());
    }

    @Test
    void invalidJsonAndHttpErrorsFallBackToDictionary() {
        RestClient.Builder invalidJsonBuilder = RestClient.builder();
        MockRestServiceServer invalidJsonServer = MockRestServiceServer.bindTo(invalidJsonBuilder).build();
        invalidJsonServer.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis invalidJson = newInterpreter(invalidJsonBuilder, "test-key").interpret(
                ConversationStage.INGREDIENTS, "一个鸡蛋", RecommendationConversationContext.empty());
        invalidJsonServer.verify();

        RestClient.Builder httpErrorBuilder = RestClient.builder();
        MockRestServiceServer httpErrorServer = MockRestServiceServer.bindTo(httpErrorBuilder).build();
        httpErrorServer.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withServerError());

        ConversationAnswerAnalysis httpError = newInterpreter(httpErrorBuilder, "test-key").interpret(
                ConversationStage.INGREDIENTS, "一个鸡蛋", RecommendationConversationContext.empty());
        httpErrorServer.verify();

        assertEquals(List.of("鸡蛋"),
                invalidJson.availableIngredients().stream().map(AvailableIngredientInput::name).toList());
        assertEquals(List.of("鸡蛋"),
                httpError.availableIngredients().stream().map(AvailableIngredientInput::name).toList());
    }

    @Test
    void localInvalidQuantityCannotBeOverriddenByAiQuantity() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(aiChickenResponse(), MediaType.APPLICATION_JSON));

        HttpConversationAnswerInterpreter interpreter = newInterpreter(builder, "test-key");
        ConversationAnswerAnalysis invalid = interpreter.interpret(
                ConversationStage.INGREDIENTS, "0克鸡胸肉", RecommendationConversationContext.empty());

        server.verify();
        assertTrue(invalid.conflicts().contains("鸡胸肉数量无效"));
        assertTrue(invalid.availableIngredients().isEmpty());

        RecommendationConversationContext blocked =
                RecommendationConversationContext.empty().merge(invalid);
        ConversationAnswerAnalysis corrected = new DictionaryConversationAnswerInterpreter().interpret(
                ConversationStage.INGREDIENTS, "300克鸡胸肉", blocked);

        assertTrue(corrected.conflicts().isEmpty());
        assertEquals(new BigDecimal("300"), corrected.availableIngredients().get(0).quantity());
    }

    @Test
    void preservesValidAiOnlyFieldsWhenLocalDictionaryHasNoMatch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(aiOnlyResponse(), MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.INGREDIENTS, "紫甘蓝", RecommendationConversationContext.empty());

        server.verify();
        assertTrue(analysis.relevant());
        assertEquals(List.of("紫甘蓝"),
                analysis.availableIngredients().stream().map(AvailableIngredientInput::name).toList());
        assertEquals(List.of("乳糖"), analysis.excludedIngredients());
        assertEquals(List.of("坚果"), analysis.allergyIngredients());
    }

    @Test
    void hardRejectsSymbolsEvenWhenAiReturnsValidFields() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(aiOnlyResponse(), MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.INGREDIENTS, "@@@", RecommendationConversationContext.empty());

        server.verify();
        assertFalse(analysis.relevant());
        assertTrue(analysis.availableIngredients().isEmpty());
    }

    @Test
    void missingChoicesMessageOrContentFallsBackToDictionary() {
        assertFallbackForResponse("{\"choices\":[]}");
        assertFallbackForResponse("{\"choices\":[{}]}");
        assertFallbackForResponse("{\"choices\":[{\"message\":{}}]}");
    }

    @Test
    void aiFalseCannotOverrideLocallyRecognizedIngredients() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(aiFalseResponse(), MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.INGREDIENTS, "300克鸡胸肉", RecommendationConversationContext.empty());

        server.verify();
        assertTrue(analysis.relevant());
        assertEquals(List.of("鸡胸肉"),
                analysis.availableIngredients().stream().map(AvailableIngredientInput::name).toList());
    }

    @Test
    void rejectsUnsupportedAiUnitsAndFalseQuantityKnownCombinations() {
        assertInvalidAiIngredient(responseFor("紫甘蓝", "200", "minutes", "true", "[]", "[]"));
        assertInvalidAiIngredient(responseFor("紫甘蓝", "-2", "g", "false", "[]", "[]"));
    }

    @Test
    void trimsAndNormalizesSupportedAiUnits() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(
                        responseFor("紫甘蓝", "200", " g ", "true", "[]", "[]"),
                        MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.INGREDIENTS, "紫甘蓝", RecommendationConversationContext.empty());

        server.verify();
        assertEquals(new BigDecimal("200"), analysis.availableIngredients().get(0).quantity());
        assertEquals("g", analysis.availableIngredients().get(0).unit());
        assertTrue(analysis.conflicts().isEmpty());
    }

    @Test
    void rejectsSymbolOnlyAiIngredientNames() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(
                        responseFor("@@@", "200", "g", "true", "[]", "[]"),
                        MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.INTENT, "请帮我推荐一道菜", RecommendationConversationContext.empty());

        server.verify();
        assertTrue(analysis.availableIngredients().isEmpty());
        assertTrue(analysis.conflicts().contains("食材名称无效"));
    }

    @Test
    void rejectsObviousAiPlaceholderIngredientNames() {
        assertInvalidAiIngredientName("abc");
        assertInvalidAiIngredientName("测试东西");
    }

    @Test
    void meaninglessAiOnlyIngredientCannotMakeAnswerRelevant() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(
                        responseFor("abc", "200", "g", "true", "[]", "[]"),
                        MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.INGREDIENTS, "请记录一下", RecommendationConversationContext.empty());

        server.verify();
        assertFalse(analysis.relevant());
        assertTrue(analysis.availableIngredients().isEmpty());
        assertTrue(analysis.conflicts().contains("食材名称无效"));
    }

    @Test
    void filtersMeaninglessAiRestrictionNamesButKeepsLegalValues() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(aiMixedRestrictionResponse(), MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.RESTRICTIONS, "我有一些饮食限制", RecommendationConversationContext.empty());

        server.verify();
        assertEquals(List.of("花生"), analysis.excludedIngredients());
        assertEquals(List.of("坚果"), analysis.allergyIngredients());
        assertTrue(analysis.conflicts().contains("食材名称无效"));
    }

    @Test
    void preservesAiOnlyExplicitNoRestrictionAnswer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(aiOnlyNoRestrictionResponse(), MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.RESTRICTIONS,
                "我没有任何饮食限制",
                RecommendationConversationContext.empty());

        server.verify();
        assertTrue(analysis.relevant());
        assertTrue(analysis.restrictionsAnswered());
        assertTrue(analysis.excludedIngredients().isEmpty());
        assertTrue(analysis.allergyIngredients().isEmpty());
    }

    @Test
    void resolvesConversationAnswerInterpreterToHttpPrimaryBean() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(InterpreterBeanConfig.class)) {
            ConversationAnswerInterpreter interpreter = context.getBean(ConversationAnswerInterpreter.class);

            assertSame(HttpConversationAnswerInterpreter.class, interpreter.getClass());
            assertEquals(2, context.getBeansOfType(ConversationAnswerInterpreter.class).size());
        }
    }

    private HttpConversationAnswerInterpreter newInterpreter(RestClient.Builder builder, String apiKey) {
        return new HttpConversationAnswerInterpreter(
                builder,
                new ObjectMapper(),
                "http://localhost",
                apiKey,
                "test-model"
        );
    }

    private RecommendationConversationContext contextWithChicken() {
        return new RecommendationConversationContext(
                "清淡饮食", null,
                List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
                List.of(), List.of(), null, null, List.of(), List.of(), false
        );
    }

    private String aiResponse() {
        return """
                {
                  "choices": [{
                    "message": {"content": "{\\"relevant\\":true,\\"intentText\\":\\"做一道快手菜\\",\\"dietGoal\\":null,\\"availableIngredients\\":[{\\"name\\":\\"鸡胸\\",\\"quantity\\":300,\\"unit\\":\\"g\\",\\"quantityKnown\\":true}],\\"excludedIngredients\\":[],\\"allergyIngredients\\":[],\\"cookingTime\\":30,\\"servings\\":1,\\"unknownTerms\\":[],\\"conflicts\\":[],\\"confidence\\":0.95,\\"restrictionsAnswered\\":true}"
                    }
                  }]
                }
                """;
    }

    private void assertFallbackForResponse(String response) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.INGREDIENTS, "一个鸡蛋", RecommendationConversationContext.empty());

        server.verify();
        assertEquals(List.of("鸡蛋"),
                analysis.availableIngredients().stream().map(AvailableIngredientInput::name).toList());
    }

    private String aiChickenResponse() {
        return responseFor("鸡胸肉", "300", "g", "true", "[]", "[]");
    }

    private String aiOnlyResponse() {
        return """
                {
                  "choices": [{
                    "message": {"content": "{\\"relevant\\":true,\\"intentText\\":null,\\"dietGoal\\":null,\\"availableIngredients\\":[{\\"name\\":\\"紫甘蓝\\",\\"quantity\\":200,\\"unit\\":\\"g\\",\\"quantityKnown\\":true}],\\"excludedIngredients\\":[\\"乳糖\\"],\\"allergyIngredients\\":[\\"坚果\\"],\\"cookingTime\\":null,\\"servings\\":null,\\"unknownTerms\\":[],\\"conflicts\\":[],\\"confidence\\":0.9,\\"restrictionsAnswered\\":true}"
                    }
                  }]
                }
                """;
    }

    private String aiFalseResponse() {
        return """
                {
                  "choices": [{
                    "message": {"content": "{\"relevant\":false,\"intentText\":null,\"dietGoal\":null,\"availableIngredients\":[],\"excludedIngredients\":[],\"allergyIngredients\":[],\"cookingTime\":null,\"servings\":null,\"unknownTerms\":[],\"conflicts\":[],\"confidence\":0.2,\"restrictionsAnswered\":false}"
                    }
                  }]
                }
                """;
    }

    private String aiOnlyNoRestrictionResponse() {
        return """
                {
                  "choices": [{
                    "message": {"content": "{\\"relevant\\":true,\\"intentText\\":null,\\"dietGoal\\":null,\\"availableIngredients\\":[],\\"excludedIngredients\\":[],\\"allergyIngredients\\":[],\\"cookingTime\\":null,\\"servings\\":null,\\"unknownTerms\\":[],\\"conflicts\\":[],\\"confidence\\":0.9,\\"restrictionsAnswered\\":true}"
                    }
                  }]
                }
                """;
    }

    private String aiMixedRestrictionResponse() {
        return """
                {
                  "choices": [{
                    "message": {"content": "{\\"relevant\\":true,\\"intentText\\":null,\\"dietGoal\\":null,\\"availableIngredients\\":[],\\"excludedIngredients\\":[\\"@@@\\",\\"花生\\",\\"测试食材\\",\\"abc\\"],\\"allergyIngredients\\":[\\"坚果\\",\\"unknown\\",\\"none\\",\\"随便\\"],\\"cookingTime\\":null,\\"servings\\":null,\\"unknownTerms\\":[],\\"conflicts\\":[],\\"confidence\\":0.9,\\"restrictionsAnswered\\":true}"
                    }
                  }]
                }
                """;
    }

    private void assertInvalidAiIngredient(String response) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.INGREDIENTS, "紫甘蓝", RecommendationConversationContext.empty());

        server.verify();
        assertTrue(analysis.availableIngredients().isEmpty());
        assertTrue(analysis.conflicts().contains("紫甘蓝数量无效"));
    }

    private void assertInvalidAiIngredientName(String name) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withSuccess(
                        responseFor(name, "200", "g", "true", "[]", "[]"),
                        MediaType.APPLICATION_JSON));

        ConversationAnswerAnalysis analysis = newInterpreter(builder, "test-key").interpret(
                ConversationStage.INGREDIENTS, "请帮我推荐一道菜", RecommendationConversationContext.empty());

        server.verify();
        assertTrue(analysis.availableIngredients().isEmpty());
        assertTrue(analysis.conflicts().contains("食材名称无效"));
    }

    @Configuration(proxyBeanMethods = false)
    @Import({DictionaryConversationAnswerInterpreter.class, HttpConversationAnswerInterpreter.class})
    static class InterpreterBeanConfig {
        @Bean(name = "conversionService")
        ApplicationConversionService conversionService() {
            return new ApplicationConversionService();
        }

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private String responseFor(
            String name,
            String quantity,
            String unit,
            String quantityKnown,
            String excluded,
            String allergies
    ) {
        return """
                {
                  "choices": [{
                    "message": {"content": "{\\"relevant\\":true,\\"intentText\\":null,\\"dietGoal\\":null,\\"availableIngredients\\":[{\\"name\\":\\"%s\\",\\"quantity\\":%s,\\"unit\\":\\"%s\\",\\"quantityKnown\\":%s}],\\"excludedIngredients\\":%s,\\"allergyIngredients\\":%s,\\"cookingTime\\":null,\\"servings\\":null,\\"unknownTerms\\":[],\\"conflicts\\":[],\\"confidence\\":0.9,\\"restrictionsAnswered\\":true}"
                    }
                  }]
                }
                """.formatted(name, quantity, unit, quantityKnown, excluded, allergies);
    }
}
