package com.shanzai.recipe.modules.recommendation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("recommendation_history")
public class RecommendationHistoryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String inputIngredients;
    private String excludedIngredients;
    private String conversationContextJson;
    private String dietGoal;
    private Integer cookingTime;
    private Integer servings;
    private String resultRecipeIds;
    private String resultDetailJson;
    private String aiSummary;
    private String aiHealthTip;
    private String aiShoppingTip;
    private Boolean aiGenerated;
    private LocalDateTime createdAt;

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

    public String getInputIngredients() {
        return inputIngredients;
    }

    public void setInputIngredients(String inputIngredients) {
        this.inputIngredients = inputIngredients;
    }

    public String getExcludedIngredients() {
        return excludedIngredients;
    }

    public void setExcludedIngredients(String excludedIngredients) {
        this.excludedIngredients = excludedIngredients;
    }

    public String getConversationContextJson() {
        return conversationContextJson;
    }

    public void setConversationContextJson(String conversationContextJson) {
        this.conversationContextJson = conversationContextJson;
    }

    public String getDietGoal() {
        return dietGoal;
    }

    public void setDietGoal(String dietGoal) {
        this.dietGoal = dietGoal;
    }

    public Integer getCookingTime() {
        return cookingTime;
    }

    public void setCookingTime(Integer cookingTime) {
        this.cookingTime = cookingTime;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public String getResultRecipeIds() {
        return resultRecipeIds;
    }

    public void setResultRecipeIds(String resultRecipeIds) {
        this.resultRecipeIds = resultRecipeIds;
    }

    public String getResultDetailJson() {
        return resultDetailJson;
    }

    public void setResultDetailJson(String resultDetailJson) {
        this.resultDetailJson = resultDetailJson;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiHealthTip() {
        return aiHealthTip;
    }

    public void setAiHealthTip(String aiHealthTip) {
        this.aiHealthTip = aiHealthTip;
    }

    public String getAiShoppingTip() {
        return aiShoppingTip;
    }

    public void setAiShoppingTip(String aiShoppingTip) {
        this.aiShoppingTip = aiShoppingTip;
    }

    public Boolean getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
