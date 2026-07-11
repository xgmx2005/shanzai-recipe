SET @has_avatar_theme = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'avatar_theme'
);
SET @avatar_theme_sql = IF(
    @has_avatar_theme = 0,
    'ALTER TABLE `user` ADD COLUMN avatar_theme VARCHAR(30) NOT NULL DEFAULT ''leaf'' AFTER nickname',
    'SELECT 1'
);
PREPARE avatar_theme_stmt FROM @avatar_theme_sql;
EXECUTE avatar_theme_stmt;
DEALLOCATE PREPARE avatar_theme_stmt;

UPDATE `user`
SET avatar_theme = 'leaf'
WHERE avatar_theme IS NULL OR avatar_theme = '';

SET @has_avatar_url = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'avatar_url'
);
SET @avatar_url_sql = IF(
    @has_avatar_url = 0,
    'ALTER TABLE `user` ADD COLUMN avatar_url VARCHAR(255) NULL AFTER avatar_theme',
    'SELECT 1'
);
PREPARE avatar_url_stmt FROM @avatar_url_sql;
EXECUTE avatar_url_stmt;
DEALLOCATE PREPARE avatar_url_stmt;

UPDATE `user`
SET avatar_url = ''
WHERE avatar_url IS NULL;
