ALTER TABLE recommendation_history
    ADD COLUMN ai_health_tip TEXT AFTER ai_summary,
    ADD COLUMN ai_shopping_tip TEXT AFTER ai_health_tip,
    ADD COLUMN ai_generated TINYINT NOT NULL DEFAULT 0 AFTER ai_shopping_tip;
