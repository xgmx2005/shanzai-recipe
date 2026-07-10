package com.shanzai.recipe.modules.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationConversationSchemaTest {
    private String schema;

    @BeforeEach
    void loadSchema() throws IOException {
        schema = Files.readString(Path.of("src/main/resources/db/schema.sql"));
    }

    @Test
    void schemaContainsConversationAndIdempotentMessageTables() {
        assertTrue(schema.contains("recommendation_conversation ("));
        assertTrue(schema.contains("recommendation_conversation_message ("));
        assertTrue(schema.contains("UNIQUE KEY uk_conversation_client_message"));
        assertTrue(schema.contains("context_json JSON NOT NULL"));
        assertTrue(schema.contains("conversation_context_json JSON"));
        assertTrue(schema.contains("result_detail_json JSON"));
    }
}
