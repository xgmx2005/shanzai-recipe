package com.shanzai.recipe.modules.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationConversationSchemaTest {
    private String schema;
    private String data;
    private String migration;

    @BeforeEach
    void loadDatabaseScripts() throws IOException {
        schema = readSql("src/main/resources/db/schema.sql");
        data = readSql("src/main/resources/db/data.sql");
        migration = readSql("src/main/resources/db/migrations/2026-07-10-add-recommendation-conversation.sql");
    }

    @Test
    void migrationCreatesBothConversationTablesWithoutDroppingExistingData() {
        String conversationTable = """
                CREATE TABLE IF NOT EXISTS recommendation_conversation (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    stage VARCHAR(30) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    context_json JSON NOT NULL,
                    invalid_answer_count INT NOT NULL DEFAULT 0,
                    recommendation_history_id BIGINT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    KEY idx_recommendation_conversation_user_status (user_id, status),
                    CONSTRAINT fk_recommendation_conversation_user FOREIGN KEY (user_id) REFERENCES `user` (id),
                    CONSTRAINT fk_recommendation_conversation_history FOREIGN KEY (recommendation_history_id)
                        REFERENCES recommendation_history (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """.strip();
        String messageTable = """
                CREATE TABLE IF NOT EXISTS recommendation_conversation_message (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    conversation_id BIGINT NOT NULL,
                    client_message_id VARCHAR(64),
                    role VARCHAR(20) NOT NULL,
                    content TEXT NOT NULL,
                    extracted_data_json JSON,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_conversation_client_message (conversation_id, client_message_id),
                    KEY idx_conversation_message_order (conversation_id, id),
                    CONSTRAINT fk_conversation_message_conversation FOREIGN KEY (conversation_id)
                        REFERENCES recommendation_conversation (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """.strip();

        assertContains(migration, conversationTable, "migration must contain the complete conversation table definition");
        assertContains(migration, messageTable, "migration must contain the complete message table definition");
        assertEquals(2, countOccurrences(migration, "CREATE TABLE IF NOT EXISTS "),
                "migration must contain exactly two idempotent CREATE TABLE statements");
        assertFalse(migration.contains("DROP TABLE"), "migration must not drop existing tables");
    }

    @Test
    void migrationUsesMetadataGuardedPreparedStatementsForHistoryColumns() {
        String conversationContextAlter = """
                SET @has_conversation_context = (
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'recommendation_history'
                      AND COLUMN_NAME = 'conversation_context_json'
                );
                SET @context_sql = IF(
                    @has_conversation_context = 0,
                    'ALTER TABLE recommendation_history ADD COLUMN conversation_context_json JSON NULL AFTER excluded_ingredients',
                    'SELECT 1'
                );
                PREPARE context_stmt FROM @context_sql;
                EXECUTE context_stmt;
                DEALLOCATE PREPARE context_stmt;
                """.strip();
        String resultDetailAlter = """
                SET @has_result_detail = (
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'recommendation_history'
                      AND COLUMN_NAME = 'result_detail_json'
                );
                SET @result_sql = IF(
                    @has_result_detail = 0,
                    'ALTER TABLE recommendation_history ADD COLUMN result_detail_json JSON NULL AFTER result_recipe_ids',
                    'SELECT 1'
                );
                PREPARE result_stmt FROM @result_sql;
                EXECUTE result_stmt;
                DEALLOCATE PREPARE result_stmt;
                """.strip();

        assertContains(migration, conversationContextAlter,
                "conversation_context_json ALTER must remain guarded and idempotent");
        assertContains(migration, resultDetailAlter,
                "result_detail_json ALTER must remain guarded and idempotent");
    }

    @Test
    void schemaDefinesConversationKeysIndexesAndJsonColumnsExactly() {
        assertContains(schema,
                "UNIQUE KEY uk_conversation_client_message (conversation_id, client_message_id),",
                "message idempotency key must cover conversation_id and client_message_id");
        assertContains(schema,
                "KEY idx_recommendation_conversation_user_status (user_id, status),",
                "conversation lookup index must cover user_id and status");
        assertContains(schema,
                "KEY idx_conversation_message_order (conversation_id, id),",
                "message ordering index must cover conversation_id and id");
        assertContains(schema,
                "CONSTRAINT fk_recommendation_conversation_user FOREIGN KEY (user_id) REFERENCES `user` (id),",
                "conversation user foreign key must retain its full definition");
        assertContains(schema, """
                CONSTRAINT fk_recommendation_conversation_history FOREIGN KEY (recommendation_history_id)
                        REFERENCES recommendation_history (id)
                """.strip(), "conversation history foreign key must retain its full definition");
        assertContains(schema, """
                CONSTRAINT fk_conversation_message_conversation FOREIGN KEY (conversation_id)
                        REFERENCES recommendation_conversation (id)
                """.strip(), "message conversation foreign key must retain its full definition");
        assertContains(schema, "context_json JSON NOT NULL,", "conversation context must be required JSON");
        assertContains(schema, "extracted_data_json JSON,", "message extraction data must remain JSON");
        assertContains(schema, "conversation_context_json JSON,", "history conversation context must remain JSON");
        assertContains(schema, "result_detail_json JSON,", "history result detail must remain JSON");
    }

    @Test
    void schemaDropsConversationTablesBeforeRecommendationHistory() {
        assertContains(schema, """
                DROP TABLE IF EXISTS recommendation_conversation_message;
                DROP TABLE IF EXISTS recommendation_conversation;
                DROP TABLE IF EXISTS recommendation_history;
                """.strip(), "schema drop order must be message, conversation, then recommendation history");
    }

    @Test
    void dataCleansConversationTablesBeforeRecommendationHistory() {
        assertContains(data, """
                TRUNCATE TABLE recommendation_conversation_message;
                TRUNCATE TABLE recommendation_conversation;
                TRUNCATE TABLE recommendation_history;
                """.strip(), "data cleanup order must be message, conversation, then recommendation history");
    }

    private static String readSql(String path) throws IOException {
        return Files.readString(Path.of(path)).replace("\r\n", "\n");
    }

    private static int countOccurrences(String text, String expected) {
        return (text.length() - text.replace(expected, "").length()) / expected.length();
    }

    private static void assertContains(String actual, String expected, String message) {
        assertTrue(actual.contains(expected), () -> message + "\nExpected SQL fragment:\n" + expected);
    }
}
