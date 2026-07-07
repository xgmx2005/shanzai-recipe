package com.shanzai.recipe.modules.admin;

import com.shanzai.recipe.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }

    @GetMapping("/stats/popular-recipes")
    public ApiResponse<List<PopularRecipeStatResponse>> listPopularRecipes(
        @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.ok(dashboardService.listPopularRecipes(limit));
    }

    @GetMapping("/stats/diet-goals")
    public ApiResponse<List<DietGoalStatResponse>> listDietGoalStats() {
        return ApiResponse.ok(dashboardService.listDietGoalStats());
    }
}
