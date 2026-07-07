package com.shanzai.recipe.modules.profile;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("user_profile")
public class ProfileEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String gender;
    private Integer age;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private BigDecimal bmi;
    private String dietGoal;
    private String tastePreferences;
    private String avoidIngredients;
    private String allergyIngredients;
    private Integer cookingTimePreference;
    private Integer dailyCalorieTarget;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public BigDecimal getBmi() {
        return bmi;
    }

    public void setBmi(BigDecimal bmi) {
        this.bmi = bmi;
    }

    public String getDietGoal() {
        return dietGoal;
    }

    public void setDietGoal(String dietGoal) {
        this.dietGoal = dietGoal;
    }

    public String getTastePreferences() {
        return tastePreferences;
    }

    public void setTastePreferences(String tastePreferences) {
        this.tastePreferences = tastePreferences;
    }

    public String getAvoidIngredients() {
        return avoidIngredients;
    }

    public void setAvoidIngredients(String avoidIngredients) {
        this.avoidIngredients = avoidIngredients;
    }

    public String getAllergyIngredients() {
        return allergyIngredients;
    }

    public void setAllergyIngredients(String allergyIngredients) {
        this.allergyIngredients = allergyIngredients;
    }

    public Integer getCookingTimePreference() {
        return cookingTimePreference;
    }

    public void setCookingTimePreference(Integer cookingTimePreference) {
        this.cookingTimePreference = cookingTimePreference;
    }

    public Integer getDailyCalorieTarget() {
        return dailyCalorieTarget;
    }

    public void setDailyCalorieTarget(Integer dailyCalorieTarget) {
        this.dailyCalorieTarget = dailyCalorieTarget;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
