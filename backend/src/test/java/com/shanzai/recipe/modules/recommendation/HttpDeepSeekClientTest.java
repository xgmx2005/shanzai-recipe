package com.shanzai.recipe.modules.recommendation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpDeepSeekClientTest {
    @Test
    void parsesStructuredRecommendationTextFromDeepSeekResponse() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        ListAppender<ILoggingEvent> logs = startLogCapture();
        server.createContext("/chat/completions", exchange -> {
            String response = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "{\\"summary\\":\\"AI总结\\",\\"healthTip\\":\\"AI健康提示\\",\\"shoppingTip\\":\\"AI购物提示\\",\\"topRecipeReason\\":\\"AI首菜理由\\"}"
                      }
                    }
                  ]
                }
                """;
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            HttpDeepSeekClient client = newClient(server);

            AiRecommendationText text = client.generateRecommendationText(sampleContext()).orElseThrow();

            assertEquals("AI总结", text.summary());
            assertEquals("AI健康提示", text.healthTip());
            assertEquals("AI购物提示", text.shoppingTip());
            assertEquals("AI首菜理由", text.topRecipeReason());
            assertLogContains(logs, "[DEEPSEEK_AI_SUCCESS]");
        } finally {
            stopLogCapture(logs);
            server.stop(0);
        }
    }

    @Test
    void logsFallbackWhenApiKeyIsMissing() {
        ListAppender<ILoggingEvent> logs = startLogCapture();
        try {
            HttpDeepSeekClient client = new HttpDeepSeekClient(
                RestClient.builder(),
                new ObjectMapper(),
                "http://localhost",
                "",
                "test-model",
                Duration.ofMillis(100),
                Duration.ofMillis(100)
            );

            assertTrue(client.generateRecommendationText(sampleContext()).isEmpty());
            assertLogContains(logs, "[DEEPSEEK_FALLBACK_NO_KEY]");
        } finally {
            stopLogCapture(logs);
        }
    }

    @Test
    void returnsEmptyWhenDeepSeekRequestTimesOut() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();

        try {
            HttpDeepSeekClient client = newClient(server);

            assertTrue(client.generateRecommendationText(sampleContext()).isEmpty());
        } finally {
            server.stop(0);
        }
    }

    private HttpDeepSeekClient newClient(HttpServer server) {
        return new HttpDeepSeekClient(
            RestClient.builder(),
            new ObjectMapper(),
            "http://localhost:" + server.getAddress().getPort(),
            "test-key",
            "test-model",
            Duration.ofMillis(100),
            Duration.ofMillis(100)
        );
    }

    private AiRecommendationContext sampleContext() {
        return new AiRecommendationContext(
            "FAT_LOSS",
            List.of("鸡胸肉", "西兰花"),
            List.of("花生"),
            30,
            List.of(new AiRecommendationContext.RecipeSnapshot(
                "鸡胸肉西兰花轻食碗",
                92,
                420,
                "35",
                List.of("鸡胸肉", "西兰花"),
                List.of("低脂", "高蛋白")
            ))
        );
    }

    private ListAppender<ILoggingEvent> startLogCapture() {
        Logger logger = (Logger) LoggerFactory.getLogger(HttpDeepSeekClient.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void stopLogCapture(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(HttpDeepSeekClient.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    private void assertLogContains(ListAppender<ILoggingEvent> appender, String expected) {
        assertTrue(
            appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains(expected)),
            () -> "Expected log message containing: " + expected
        );
    }
}
