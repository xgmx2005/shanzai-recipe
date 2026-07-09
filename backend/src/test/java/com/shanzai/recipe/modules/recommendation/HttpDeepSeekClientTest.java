package com.shanzai.recipe.modules.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpDeepSeekClientTest {
    @Test
    void parsesStructuredRecommendationTextFromDeepSeekResponse() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
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
        } finally {
            server.stop(0);
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
}
