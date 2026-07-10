package com.shanzai.recipe.modules.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationConversationSchemaTest {
    private static final String CONVERSATION_TABLE = "recommendation_conversation";
    private static final String MESSAGE_TABLE = "recommendation_conversation_message";

    private static final List<String> CONVERSATION_TABLE_CONTRACT = List.of(
            "id BIGINT PRIMARY KEY AUTO_INCREMENT",
            "user_id BIGINT NOT NULL",
            "stage VARCHAR(30) NOT NULL",
            "status VARCHAR(30) NOT NULL",
            "context_json JSON NOT NULL",
            "invalid_answer_count INT NOT NULL DEFAULT 0",
            "recommendation_history_id BIGINT NULL",
            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP",
            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP",
            "KEY idx_recommendation_conversation_user_status (user_id, status)",
            "CONSTRAINT fk_recommendation_conversation_user FOREIGN KEY (user_id) REFERENCES `user` (id)",
            "CONSTRAINT fk_recommendation_conversation_history FOREIGN KEY (recommendation_history_id) "
                    + "REFERENCES recommendation_history (id)"
    );

    private static final List<String> MESSAGE_TABLE_CONTRACT = List.of(
            "id BIGINT PRIMARY KEY AUTO_INCREMENT",
            "conversation_id BIGINT NOT NULL",
            "client_message_id VARCHAR(64)",
            "role VARCHAR(20) NOT NULL",
            "content TEXT NOT NULL",
            "extracted_data_json JSON",
            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP",
            "UNIQUE KEY uk_conversation_client_message (conversation_id, client_message_id)",
            "KEY idx_conversation_message_order (conversation_id, id)",
            "CONSTRAINT fk_conversation_message_conversation FOREIGN KEY (conversation_id) "
                    + "REFERENCES recommendation_conversation (id)"
    );

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
    void migrationCreatesExactlyTwoConversationTablesWithoutDroppingExistingData() {
        extractCreateTableBody(migration, CONVERSATION_TABLE);
        extractCreateTableBody(migration, MESSAGE_TABLE);

        assertEquals(2, countOccurrences(normalizeWhitespace(migration), "CREATE TABLE IF NOT EXISTS "),
                "migration must contain exactly two idempotent CREATE TABLE statements");
        assertFalse(normalizeWhitespace(migration).contains("DROP TABLE"),
                "migration must not drop existing tables");
    }

    @Test
    void schemaAndMigrationDefineCompleteConversationTableContract() {
        assertTableContract(schema, "schema", CONVERSATION_TABLE, CONVERSATION_TABLE_CONTRACT);
        assertTableContract(migration, "migration", CONVERSATION_TABLE, CONVERSATION_TABLE_CONTRACT);
    }

    @Test
    void schemaAndMigrationDefineCompleteMessageTableContract() {
        assertTableContract(schema, "schema", MESSAGE_TABLE, MESSAGE_TABLE_CONTRACT);
        assertTableContract(migration, "migration", MESSAGE_TABLE, MESSAGE_TABLE_CONTRACT);
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
                """;
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
                """;

        assertContainsNormalized(migration, conversationContextAlter,
                "conversation_context_json ALTER must remain guarded and idempotent");
        assertContainsNormalized(migration, resultDetailAlter,
                "result_detail_json ALTER must remain guarded and idempotent");
    }

    @Test
    void schemaDefinesHistoryJsonColumnsInsideRecommendationHistoryTable() {
        List<String> historyClauses = splitTableClauses(
                extractCreateTableBody(schema, "recommendation_history"));

        assertTrue(historyClauses.contains("conversation_context_json JSON"),
                "recommendation_history must contain nullable conversation_context_json JSON");
        assertTrue(historyClauses.contains("result_detail_json JSON"),
                "recommendation_history must contain nullable result_detail_json JSON");
    }

    @Test
    void schemaDropsConversationTablesBeforeRecommendationHistory() {
        assertInOrder(normalizeWhitespace(schema),
                "DROP TABLE IF EXISTS recommendation_conversation_message;",
                "DROP TABLE IF EXISTS recommendation_conversation;",
                "DROP TABLE IF EXISTS recommendation_history;");
    }

    @Test
    void dataCleansConversationTablesInOrderWhileForeignKeysAreDisabled() {
        String foreignKeyDisabledBlock = extractForeignKeyDisabledBlock(data);

        assertInOrder(foreignKeyDisabledBlock,
                "TRUNCATE TABLE recommendation_conversation_message;",
                "TRUNCATE TABLE recommendation_conversation;",
                "TRUNCATE TABLE recommendation_history;");
    }

    private static String readSql(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertTableContract(
            String sql,
            String scriptName,
            String tableName,
            List<String> expectedClauses
    ) {
        List<String> actualClauses = splitTableClauses(extractCreateTableBody(sql, tableName));

        assertEquals(expectedClauses.size(), actualClauses.size(),
                () -> scriptName + " " + tableName + " must contain exactly the contracted clauses\nActual: "
                        + actualClauses);
        for (String expectedClause : expectedClauses) {
            String normalizedClause = normalizeWhitespace(expectedClause);
            assertTrue(actualClauses.contains(normalizedClause),
                    () -> scriptName + " " + tableName + " is missing clause: " + normalizedClause
                            + "\nActual: " + actualClauses);
        }
    }

    private static String extractCreateTableBody(String sql, String tableName) {
        String normalizedSql = normalizeWhitespace(sql);
        String idempotentPrefix = "CREATE TABLE IF NOT EXISTS " + tableName + " (";
        String schemaPrefix = "CREATE TABLE " + tableName + " (";
        int bodyStart = normalizedSql.indexOf(idempotentPrefix);

        if (bodyStart >= 0) {
            bodyStart += idempotentPrefix.length();
        } else {
            bodyStart = normalizedSql.indexOf(schemaPrefix);
            assertTrue(bodyStart >= 0, () -> "missing CREATE TABLE block for " + tableName);
            bodyStart += schemaPrefix.length();
        }

        int bodyEnd = normalizedSql.indexOf(") ENGINE=", bodyStart);
        assertTrue(bodyEnd > bodyStart, () -> "incomplete CREATE TABLE block for " + tableName);
        return normalizedSql.substring(bodyStart, bodyEnd).trim();
    }

    private static List<String> splitTableClauses(String tableBody) {
        List<String> clauses = new ArrayList<>();
        int parenthesesDepth = 0;
        int clauseStart = 0;

        for (int index = 0; index < tableBody.length(); index++) {
            char current = tableBody.charAt(index);
            if (current == '(') {
                parenthesesDepth++;
            } else if (current == ')') {
                parenthesesDepth--;
            } else if (current == ',' && parenthesesDepth == 0) {
                clauses.add(normalizeWhitespace(tableBody.substring(clauseStart, index)));
                clauseStart = index + 1;
            }
        }

        clauses.add(normalizeWhitespace(tableBody.substring(clauseStart)));
        return clauses;
    }

    private static String extractForeignKeyDisabledBlock(String sql) {
        String normalizedSql = normalizeWhitespace(sql);
        String disableStatement = "SET FOREIGN_KEY_CHECKS = 0;";
        String enableStatement = "SET FOREIGN_KEY_CHECKS = 1;";
        int blockStart = normalizedSql.indexOf(disableStatement);
        assertTrue(blockStart >= 0, "data.sql must disable foreign key checks before cleanup");
        blockStart += disableStatement.length();

        int blockEnd = normalizedSql.indexOf(enableStatement, blockStart);
        assertTrue(blockEnd > blockStart, "data.sql must re-enable foreign key checks after cleanup");
        return normalizedSql.substring(blockStart, blockEnd).trim();
    }

    private static void assertInOrder(String text, String... expectedStatements) {
        int previousPosition = -1;
        for (String expectedStatement : expectedStatements) {
            int currentPosition = text.indexOf(expectedStatement, previousPosition + 1);
            assertTrue(currentPosition > previousPosition,
                    () -> "expected SQL statement in order: " + expectedStatement + "\nSQL: " + text);
            previousPosition = currentPosition;
        }
    }

    private static int countOccurrences(String text, String expected) {
        return (text.length() - text.replace(expected, "").length()) / expected.length();
    }

    private static void assertContainsNormalized(String actual, String expected, String message) {
        String normalizedActual = normalizeWhitespace(actual);
        String normalizedExpected = normalizeWhitespace(expected);
        assertTrue(normalizedActual.contains(normalizedExpected),
                () -> message + "\nExpected SQL fragment: " + normalizedExpected);
    }

    private static String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
