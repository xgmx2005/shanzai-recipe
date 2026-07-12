ALTER TABLE user_profile
    ADD COLUMN profile_completed TINYINT NOT NULL DEFAULT 0 AFTER daily_calorie_target;
