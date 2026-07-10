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
